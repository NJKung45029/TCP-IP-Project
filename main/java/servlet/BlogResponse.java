package servlet; 

import java.util.List;

public class BlogResponse {
    private List<BlogItem> items;

    // Getter and Setter
    public List<BlogItem> getItems() { return items; }
    public void setItems(List<BlogItem> items) { this.items = items; }
}