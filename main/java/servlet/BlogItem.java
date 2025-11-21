package servlet; 

public class BlogItem {
    private String title;
    private String link;
    private String description;
    private String bloggername;
    private String postdate; //추가 (날짜 필터링용)
    
 // 생성자 추가(네이버 블로그 메니저의 search 부분을 작동을 위한 코드)
    public BlogItem(String title, String link, String description, String bloggername, String postdate) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.bloggername = bloggername;
        this.postdate = postdate;
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBloggername() { return bloggername; }
    public void setBloggername(String bloggername) { this.bloggername = bloggername; }
    // 날짜 getter 추가
    public String getPostdate() { return postdate; }
    public void setPostdate(String postdate) { this.postdate = postdate; }
}