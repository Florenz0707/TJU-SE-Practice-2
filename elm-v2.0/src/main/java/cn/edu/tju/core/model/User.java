package cn.edu.tju.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.BatchSize;

//import javax.persistence.*;
//import javax.validation.constraints.NotNull;
//import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends BaseEntity{

//   @JsonIgnore
//   @Id
//   @Column(name = "ID")
//   @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_SEQ")
//   @SequenceGenerator(name = "USER_SEQ", sequenceName = "USER_SEQ", allocationSize = 1)
//   @GeneratedValue(strategy = GenerationType.IDENTITY)
//   private Long id;

   @Column(name = "USERNAME", length = 100, unique = true)
   @NotNull
   @Size(min = 1, max = 100)
   private String username;

   @JsonIgnore
   @Column(name = "PASSWORD", length = 100)
   @NotNull
   @Size(min = 1, max = 100)
   private String password;

//   @Column(name = "FIRSTNAME", length = 50)
//   @NotNull
//   @Size(min = 4, max = 50)
//   private String firstname;
//
//   @Column(name = "LASTNAME", length = 50)
//   @NotNull
//   @Size(min = 4, max = 50)
//   private String lastname;
//
//   @Column(name = "EMAIL", length = 50)
//   @NotNull
//   @Size(min = 4, max = 50)
//   private String email;

   @JsonIgnore
   @Column(name = "ACTIVATED")
   @NotNull
   private boolean activated;

   @ManyToMany
   @JoinTable(
      name = "USER_AUTHORITY",
      joinColumns = {@JoinColumn(name = "USER_ID", referencedColumnName = "ID")},
      inverseJoinColumns = {@JoinColumn(name = "AUTHORITY_NAME", referencedColumnName = "NAME")})
   @BatchSize(size = 20)
   private Set<Authority> authorities = new HashSet<>();

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public boolean isActivated() {
      return activated;
   }

   public void setActivated(boolean activated) {
      this.activated = activated;
   }

   public Set<Authority> getAuthorities() {
      return authorities;
   }

   public void setAuthorities(Set<Authority> authorities) {
      this.authorities = authorities;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      User user = (User) o;
      return this.getId().equals(user.getId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.getId());
   }

   @Override
   public String toString() {
      return "User{" +
         "username='" + username + '\'' +
         ", password='" + password + '\'' +
//         ", firstname='" + firstname + '\'' +
//         ", lastname='" + lastname + '\'' +
//         ", email='" + email + '\'' +
         ", activated=" + activated +
         '}';
   }
}
