package cn.edu.tju.merchant.model;

public class User extends BaseEntity {
    private java.util.Set<String> authorities = new java.util.HashSet<>();
    public void setAuthorities(java.util.Set<String> auths) { this.authorities = auths; }
    public java.util.Set<String> getAuthorities() { return authorities; }

    private String username;
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
