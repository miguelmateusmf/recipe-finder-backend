package com.miguel.backend_for_front.user;

import com.miguel.backend_for_front.user.dto.ChangePasswordRequest;
import com.miguel.backend_for_front.user.dto.UpdateEmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).email("me@test.com").password("hashed-old")
                .firstName("Old").lastName("Name").role(Role.USER).build();
    }

    @Test
    void changePassword_updatesWhenCurrentPasswordCorrect() {
        when(passwordEncoder.matches("current", "hashed-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("hashed-new");

        userService.changePassword(user, new ChangePasswordRequest("current", "new-password"));

        verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed-new")));
    }

    @Test
    void changePassword_throwsWhenCurrentPasswordWrong() {
        when(passwordEncoder.matches("wrong", "hashed-old")).thenReturn(false);

        assertThatThrownBy(() ->
                userService.changePassword(user, new ChangePasswordRequest("wrong", "new")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmail_throwsWhenEmailAlreadyInUse() {
        when(passwordEncoder.matches("pw", "hashed-old")).thenReturn(true);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.updateEmail(user, new UpdateEmailRequest("taken@test.com", "pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void updateEmail_allowsKeepingOwnEmail() {
        when(passwordEncoder.matches("pw", "hashed-old")).thenReturn(true);
        when(userRepository.existsByEmail("me@test.com")).thenReturn(true); // yourself

        // Should NOT throw — you can "change" to your own email
        userService.updateEmail(user, new UpdateEmailRequest("me@test.com", "pw"));

        verify(userRepository).save(any());
    }
}