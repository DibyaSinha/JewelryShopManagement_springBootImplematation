package org.example.service;

import org.example.entity.Staff;
import org.example.entity.User;
import org.example.repository.StaffRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    public Staff getStaffById(Long id) {
        return staffRepository.findById(id).orElse(null);
    }

    @Transactional
    public Staff createStaff(Staff staff) {
        if (staffRepository.findByMobileNumber(staff.getMobileNumber()).isPresent()) {
            throw new IllegalArgumentException("Mobile number already registered: " + staff.getMobileNumber());
        }
        if (staffRepository.findByAadhaarNumber(staff.getAadhaarNumber()).isPresent()) {
            throw new IllegalArgumentException("Aadhaar number already registered: " + staff.getAadhaarNumber());
        }

        if (staff.getLoginAccess() != null && staff.getLoginAccess()) {
            if (staff.getUsername() == null || staff.getUsername().trim().isEmpty() || staff.getPassword() == null || staff.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Username and Password are required for login access");
            }
            
            if (userRepository.findByUsername(staff.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username already exists: " + staff.getUsername());
            }

            // Hash password for staff table
            staff.setPassword(passwordEncoder.encode(staff.getPassword()));

            // Create corresponding User entity for authentication
            User user = new User();
            user.setUsername(staff.getUsername());
            user.setPassword(staff.getPassword()); // Already hashed
            user.setRole("STAFF");
            userRepository.save(user);
        } else {
            staff.setUsername(null);
            staff.setPassword(null);
        }
        return staffRepository.save(staff);
    }

    @Transactional
    public Staff updateStaff(Long id, Staff details) {
        Staff existing = staffRepository.findById(id).orElseThrow(() -> new RuntimeException("Staff not found"));

        // Check for duplicates in other records
        staffRepository.findByMobileNumber(details.getMobileNumber()).ifPresent(s -> {
            if (!s.getId().equals(id)) throw new IllegalArgumentException("Mobile number already in use by another staff: " + details.getMobileNumber());
        });
        staffRepository.findByAadhaarNumber(details.getAadhaarNumber()).ifPresent(s -> {
            if (!s.getId().equals(id)) throw new IllegalArgumentException("Aadhaar number already in use by another staff: " + details.getAadhaarNumber());
        });

        // Handle login access changes
        boolean hadAccess = existing.getLoginAccess() != null && existing.getLoginAccess();
        boolean wantsAccess = details.getLoginAccess() != null && details.getLoginAccess();

        if (!hadAccess && wantsAccess) {
            // Enabling access
            if (details.getUsername() == null || details.getUsername().trim().isEmpty() || details.getPassword() == null || details.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Username and Password are required to enable login access");
            }

            if (userRepository.findByUsername(details.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username already exists: " + details.getUsername());
            }

            String hashedPassword = passwordEncoder.encode(details.getPassword());
            existing.setUsername(details.getUsername());
            existing.setPassword(hashedPassword);

            User user = new User();
            user.setUsername(details.getUsername());
            user.setPassword(hashedPassword);
            user.setRole("STAFF");
            userRepository.save(user);
        } else if (hadAccess && !wantsAccess) {
            // Disabling access
            userRepository.findByUsername(existing.getUsername()).ifPresent(userRepository::delete);
            existing.setUsername(null);
            existing.setPassword(null);
        } else if (hadAccess && wantsAccess) {
            // Maintaining access, check if username changed
            if (details.getUsername() == null || details.getUsername().trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be empty");
            }

            if (!existing.getUsername().equals(details.getUsername())) {
                if (userRepository.findByUsername(details.getUsername()).isPresent()) {
                    throw new IllegalArgumentException("Username already exists: " + details.getUsername());
                }

                User user = userRepository.findByUsername(existing.getUsername()).orElseThrow();
                user.setUsername(details.getUsername());
                userRepository.save(user);
                existing.setUsername(details.getUsername());
            }
        }

        existing.setName(details.getName());
        existing.setMobileNumber(details.getMobileNumber());
        existing.setAadhaarNumber(details.getAadhaarNumber());
        existing.setGender(details.getGender());
        existing.setSalary(details.getSalary());
        existing.setLoginAccess(details.getLoginAccess());

        return staffRepository.save(existing);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        Staff staff = staffRepository.findById(id).orElseThrow(() -> new RuntimeException("Staff not found"));
        if (staff.getLoginAccess() != null && staff.getLoginAccess()) {
            String hashedPassword = passwordEncoder.encode(newPassword);
            staff.setPassword(hashedPassword);
            
            User user = userRepository.findByUsername(staff.getUsername()).orElseThrow();
            user.setPassword(hashedPassword);
            
            userRepository.save(user);
            staffRepository.save(staff);
        } else {
            throw new RuntimeException("Staff does not have login access enabled");
        }
    }

    @Transactional
    public void deleteStaff(Long id) {
        Staff staff = staffRepository.findById(id).orElseThrow();
        if (staff.getLoginAccess() != null && staff.getLoginAccess()) {
            userRepository.findByUsername(staff.getUsername()).ifPresent(userRepository::delete);
        }
        staffRepository.deleteById(id);
    }
}
