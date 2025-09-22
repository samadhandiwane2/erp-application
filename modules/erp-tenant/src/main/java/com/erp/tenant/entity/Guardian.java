package com.erp.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "guardians")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_type", nullable = false)
    private GuardianType guardianType;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "annual_income", precision = 12, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "office_address", columnDefinition = "TEXT")
    private String officeAddress;

    @Column(name = "office_phone", length = 20)
    private String officePhone;

    @Column(name = "aadhar_number", length = 12)
    private String aadharNumber;

    @Column(name = "is_primary_contact")
    private Boolean isPrimaryContact = false;

    @Column(name = "can_pickup_child")
    private Boolean canPickupChild = true;

    @Column(name = "photo_url")
    private String photoUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public enum GuardianType {
        FATHER, MOTHER, GUARDIAN, OTHER
    }

}
