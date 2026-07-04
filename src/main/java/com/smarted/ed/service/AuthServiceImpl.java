package com.smarted.ed.service;

import com.smarted.ed.configs.JwtUtils;
import com.smarted.ed.dto.LoginRequest;
import com.smarted.ed.dto.LoginResponse;
import com.smarted.ed.dto.SignupRequest;
import com.smarted.ed.dto.UserDto;
import com.smarted.ed.entity.TutorProfile;
import com.smarted.ed.entity.User;
import com.smarted.ed.entity.VerificationToken;
import com.smarted.ed.enums.ApprovalStatus;
import com.smarted.ed.enums.RoleType;
import com.smarted.ed.exception.EmailAlreadyExistsException;
import com.smarted.ed.exception.InvalidTokenException;
import com.smarted.ed.exception.UserNotActiveException;
import com.smarted.ed.exception.UserNotVerifiedException;
import com.smarted.ed.repository.UserRepository;
import com.smarted.ed.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        // 1. Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email này đã được sử dụng");
        }

        // Validate role (only allow TUTOR or PARENT for self-registration)
        if (request.getRole() != RoleType.TUTOR && request.getRole() != RoleType.PARENT) {
            throw new IllegalArgumentException("Vai trò đăng ký không hợp lệ");
        }

        // 2. Create User entity with emailVerified = false
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setEmailVerified(false);
        user.setIsActive(true);

        // If user is TUTOR, build a default profile as required by DB mapping
        if (request.getRole() == RoleType.TUTOR) {
            TutorProfile tutorProfile = new TutorProfile();
            tutorProfile.setUser(user);
            tutorProfile.setApprovalStatus(ApprovalStatus.PENDING);
            tutorProfile.setAverageRating(BigDecimal.ZERO);
            user.setTutorProfile(tutorProfile);
        }

        userRepository.save(user);

        // 3. Create verification token (UUID) expiring in 24h
        String tokenStr = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(
                tokenStr,
                user,
                LocalDateTime.now().plusHours(24)
        );
        verificationTokenRepository.save(verificationToken);

        // 4. Send verification email (non-blocking thanks to @Async)
        emailService.sendVerificationEmail(user.getEmail(), tokenStr, user.getFullName());
    }

    @Override
    @Transactional
    public void verifyEmail(String tokenStr) {
        // 1. Find token
        VerificationToken verificationToken = verificationTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new InvalidTokenException("Mã xác thực không hợp lệ hoặc không tồn tại"));

        // 2. Check if already used
        if (Boolean.TRUE.equals(verificationToken.getUsed())) {
            throw new InvalidTokenException("Mã xác thực này đã được sử dụng");
        }

        // 3. Check if expired
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Mã xác thực đã hết hạn");
        }

        // 4. If valid, set emailVerified = true & token used
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 1. Fetch user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác"));

        // 2. Match password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }

        // 3. Check if email is verified
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UserNotVerifiedException("Tài khoản chưa được xác thực email. Vui lòng kiểm tra hộp thư");
        }

        // 4. Check if active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UserNotActiveException("Tài khoản của bạn đã bị vô hiệu hóa");
        }

        // 5. Generate JWT token
        String jwtToken = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // 6. Build UserDto response
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();

        return new LoginResponse(jwtToken, userDto);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        // 1. Fetch user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        // 2. Check if already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Tài khoản này đã được xác thực");
        }

        // 3. Create verification token (UUID) expiring in 24h
        String tokenStr = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(
                tokenStr,
                user,
                LocalDateTime.now().plusHours(24)
        );
        verificationTokenRepository.save(verificationToken);

        // 4. Send verification email
        emailService.sendVerificationEmail(user.getEmail(), tokenStr, user.getFullName());
    }
}
