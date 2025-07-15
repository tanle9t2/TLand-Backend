package com.tanle.tland.user_service.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private String id;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "email")
    private String email;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "dob")
    private LocalDate dob;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "last_access")
    private LocalDateTime lastAccess;
    @Column(name = "is_active")
    private boolean isActive;
    @Column(name = "sex")
    private boolean sex;
    @Column(name = "avt_url")
    private String avtUrl;
    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "follower",
            joinColumns = @JoinColumn(name = "user_id"),  // this user
            inverseJoinColumns = @JoinColumn(name = "follower_id")  // the follower
    )
    private Set<User> followers;

    @ManyToMany(mappedBy = "followers")
    @JsonIgnore
    private Set<User> following;

    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

}
