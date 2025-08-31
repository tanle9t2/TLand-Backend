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
    @Column(name = "user_id")
    private String id;
    @Column(name = "username")
    private String username;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "email")
    private String email;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "dob")
    private LocalDate dob;
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "last_access")
    private LocalDateTime lastAccess;
    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
    @Column(name = "sex")
    private boolean sex;
    @Column(name = "avt_url")
    private String avtUrl;

    @Column(name = "banner_url")
    private String bannerUrl;
    @Column(name = "description")
    private String description;

    @Column(name = "tax_code")
    private String taxCode;

    @Column(name = "cid")
    private String cid;

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


}
