package cn.edu.tju.merchant.model;

public class User extends BaseEntity {
    private java.util.Set<String> authorities = new java.util.HashSet<>();
    public void setAuthorities(java.util.Set<String> auths) { this.authorities = auths; }
    public java.util.Set<String> getAuthorities() { return authorities; }

    private Long id;
    private String username;
    // getters setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
