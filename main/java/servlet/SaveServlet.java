package servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson; // Gson 임포트

// 서블릿 매핑 (web.xml 또는 어노테이션) 
public class SaveServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        
        request.setCharacterEncoding("UTF-8");
        String keyword = request.getParameter("keyword");
        
        // [추가] 정렬 옵션 받기 (최신순으로 보고 있었다면 최신순으로 100개 저장해야 함)
        String sort = request.getParameter("sort");
        if(sort == null || sort.isEmpty()) sort = "sim";
        
        // 1. Naver API 호출 (SearchServlet과 거의 동일)
        String clientId = "q9X2XFTC9LvXPI2PUqgm"; 
        String clientSecret = "UPCseGEtfF";
        String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
        
        // [수정] 정렬(sort) 파라미터 추가 (display는 100 고정)
        String apiURL = "https://openapi.naver.com/v1/search/blog.json?query=" + encodedKeyword + "&display=100&sort=" + sort;
        
        String responseBody = "";
        try {
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            
            int responseCode = con.getResponseCode();
            BufferedReader br = (responseCode == 200) ? 
                new BufferedReader(new InputStreamReader(con.getInputStream())) :
                new BufferedReader(new InputStreamReader(con.getErrorStream()));
            
            String inputLine;
            StringBuffer apiResponse = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                apiResponse.append(inputLine);
            }
            br.close();
            responseBody = apiResponse.toString();
        } catch (Exception e) {
            request.setAttribute("message", "API 호출 실패: " + e.getMessage());
        }

        // 2. Gson으로 JSON 파싱 (SearchServlet과 동일)
        List<BlogItem> items = null;
        try {
            Gson gson = new Gson();
            BlogResponse blogResponse = gson.fromJson(responseBody, BlogResponse.class);
            items = blogResponse.getItems();
         // [추가] 저장하기 전에 <b> 태그 등 불필요한 문자 제거
            if(items != null) {
                for(BlogItem item : items) {
                    item.setTitle(removeHtmlTag(item.getTitle()));
                    item.setDescription(removeHtmlTag(item.getDescription()));
                }
            }
        } catch (Exception e) {
             request.setAttribute("message", "JSON 파싱 실패: " + e.getMessage());
        }

        // ---------------------------------------------------------------
        // 3. ★★★ 기억하신 DB 저장 로직 ★★★
        // ---------------------------------------------------------------
        String dbMessage = "";
        if (items != null) {
            Connection conn = null;
            PreparedStatement stmt = null;
            int insertCount = 0;
            
            try {
                // [기억한 코드] 1. 드라이버 로드
                Class.forName("com.mysql.cj.jdbc.Driver");
                // [기억한 코드] 2. 커넥션 (DB명, ID, PW 확인)
                conn = DriverManager.getConnection("jdbc:mysql://localhost/testdb1","root","1234");
                // [수정] postdate(작성일) 컬럼 추가
                String sql = "INSERT INTO naver_search_results (keyword, title, link, description, bloggername, postdate) VALUES (?, ?, ?, ?, ?, ?)";
                stmt = conn.prepareStatement(sql);
                
                for(BlogItem item : items) {
                    stmt.setString(1, keyword); 
                    stmt.setString(2, item.getTitle());
                    stmt.setString(3, item.getLink());
                    stmt.setString(4, item.getDescription());
                    stmt.setString(5, item.getBloggername());
                    // [추가] 작성일 데이터 세팅
                    stmt.setString(6, item.getPostdate());
                    
                    // [수정] 배치 처리로 성능 향상 (한 번에 모아서 전송)
                    // addBatch(): 쿼리를 모아두는 역할
                    stmt.addBatch();
                    insertCount++;
                }
                // [수정] 모아둔 쿼리 일괄 실행
                //executeBatch()가 실제로 DB에 저장하는 시점
                stmt.executeBatch();
                
                dbMessage = "DB 저장 성공! 총 " + insertCount + "건이 저장되었습니다.";
                
            } catch(Exception e) {
                e.printStackTrace();
                dbMessage = "DB 저장 실패: " + e.getMessage();
            } finally {
                // [기억한 코드] 5. 자원 해제
                try { if(stmt != null) stmt.close(); } catch (Exception e) {}
                try { if(conn != null) conn.close(); } catch (Exception e) {}
            }
        }
        
        // 4. 결과를 save.jsp로 전달
        request.setAttribute("message", dbMessage);
        // [추가] keyword와 sort를 다시 JSP로 보내서 원래 상태 유지 가능하게 함
        // 해당 코드를 추가함으로 검색버튼을 눌러도 검색창에 검색어가 계속 남아있게 됨
        request.setAttribute("keyword", keyword);
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("save.jsp");
        dispatcher.forward(request, response);
    }

 // [추가] HTML 태그 제거를 위한 내부 메서드
    private String removeHtmlTag(String text) {
        if(text == null) return "";
        return text.replaceAll("<b>", "").replaceAll("</b>", "").replaceAll("&quot;", "\"").replaceAll("&gt;", ">").replaceAll("&lt;", "<");
    }
}
