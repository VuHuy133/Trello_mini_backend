package com.trello.service;

import com.trello.entity.User;
import com.trello.entity.ProjectMember;
import com.trello.exception.ResourceNotFoundException;
import com.trello.exception.UserAlreadyExistsException;
import com.trello.repository.UserRepository;
import com.trello.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProjectMemberRepository projectMemberRepository;

	// ===== User Registration & Login =====

	public User registerUser(String username, String email, String password) {
		if (userRepository.existsByUsername(username)) {
			throw new UserAlreadyExistsException("username", username);
		}
		if (userRepository.existsByEmail(email)) {
			throw new UserAlreadyExistsException("email", email);
		}

		User user = User.builder()
				.username(username)
				.email(email)
				.password(passwordEncoder.encode(password))
				.role("USER")
				.createdAt(LocalDateTime.now())
				.build();
		
		return userRepository.save(user);
	}

	public User handleRegister(User user) {
		if (userRepository.existsByUsername(user.getUsername())) {
			throw new UserAlreadyExistsException("username", user.getUsername());
		}
		if (userRepository.existsByEmail(user.getEmail())) {
			throw new UserAlreadyExistsException("email", user.getEmail());
		}

		String hashPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(hashPassword);
		user.setRole("USER");
		user.setCreatedAt(LocalDateTime.now());

		return userRepository.save(user);
	}

	// ===== User CRUD Operations =====

	public User getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
	}

	public Optional<User> getUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	public User findUserByEmail(String email) {
		Optional<User> userOpt = userRepository.findByEmail(email);
		return userOpt.orElse(null);
	}

	public Optional<User> getUserByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	@Transactional(readOnly = true)
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	public List<User> fetchUsers() {
		return userRepository.findAll();
	}

	// ===== User Creation (Admin) =====

	public User createUser(User user) {
		if (userRepository.existsByUsername(user.getUsername())) {
			throw new UserAlreadyExistsException("username", user.getUsername());
		}
		if (userRepository.existsByEmail(user.getEmail())) {
			throw new UserAlreadyExistsException("email", user.getEmail());
		}

		String hashPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(hashPassword);
		
		if (user.getRole() == null || user.getRole().isEmpty()) {
			user.setRole("USER");
		}
		
		user.setCreatedAt(LocalDateTime.now());

		return userRepository.save(user);
	}

	// ===== OAuth2 User Save (skip duplicate check & skip password encoding) =====

	public User saveOAuth2User(String email, String name) {
		return userRepository.findByEmail(email).orElseGet(() -> {
			User user = User.builder()
					.email(email)
					.username(email)
					.password("")
					.name(name)
					.role("USER")
					.createdAt(LocalDateTime.now())
					.build();
			return userRepository.save(user);
		});
	}

	// ===== User Update =====

	public void updateUser(User inputUser) {
		User currentUserInDB = getUserById(inputUser.getId());
		
		if (currentUserInDB != null) {
			if (inputUser.getUsername() != null && !inputUser.getUsername().isEmpty()) {
				if (!inputUser.getUsername().equals(currentUserInDB.getUsername())) {
					if (userRepository.existsByUsername(inputUser.getUsername())) {
						throw new UserAlreadyExistsException("username", inputUser.getUsername());
					}
				}
				currentUserInDB.setUsername(inputUser.getUsername());
			}

			if (inputUser.getEmail() != null && !inputUser.getEmail().isEmpty()) {
				if (!inputUser.getEmail().equals(currentUserInDB.getEmail())) {
					if (userRepository.existsByEmail(inputUser.getEmail())) {
						throw new UserAlreadyExistsException("email", inputUser.getEmail());
					}
				}
				currentUserInDB.setEmail(inputUser.getEmail());
			}

			userRepository.save(currentUserInDB);
		}
	}

	public User updateUser(Long id, String username, String email) {
		User user = getUserById(id);

		if (email != null && !email.equals(user.getEmail())) {
			if (userRepository.existsByEmail(email)) {
				throw new UserAlreadyExistsException("email", email);
			}
			user.setEmail(email);
		}

		if (username != null && !username.equals(user.getUsername())) {
			if (userRepository.existsByUsername(username)) {
				throw new UserAlreadyExistsException("username", username);
			}
			user.setUsername(username);
		}

		return userRepository.save(user);
	}

	// ===== User Deletion =====

	public void updateRole(Long userId, String role) {
		User user = getUserById(userId);
		user.setRole(role);
		userRepository.save(user);
	}

	public void deleteUser(Long id) {
		if (!userRepository.existsById(id)) {
			throw new ResourceNotFoundException("User", "id", id);
		}
		userRepository.deleteById(id);
	}

	// ===== User Projects (Joined Projects) =====

	@Transactional(readOnly = true)
	public List<ProjectMember> getUserJoinedProjects(Long userId) {
		User user = getUserById(userId);
		return projectMemberRepository.findByUser(user);
	}

	// ===== User Validation =====

	@Transactional(readOnly = true)
	public boolean existsByEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	@Transactional(readOnly = true)
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	public boolean isEmailExist(String email) {
		return userRepository.existsByEmail(email);
	}

	// ===== Password Operations =====

	public boolean validatePassword(String rawPassword, String encodedPassword) {
		return passwordEncoder.matches(rawPassword, encodedPassword);
	}

	public String encodePassword(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	public void updatePassword(Long userId, String newEncodedPassword) {
		User user = getUserById(userId);
		user.setPassword(newEncodedPassword);
		userRepository.save(user);
	}
}
