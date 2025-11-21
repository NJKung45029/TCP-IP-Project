package servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
// 'WebServlet' 어노테이션은 사용하지 않습니다. web.xml로 관리합니다.
// import javax.servlet.annotation.WebServlet; 
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson; // Gson 임포트


public class SearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // GET 방식으로 검색어를 받으므로 doPost가 아닌 doGet을 구현합니다.
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        // 1. 검색어 받기 (검색어 + 페이징/필터 옵션 추가)
        String keyword = request.getParameter("keyword");
        
        // [추가] 정렬 옵션 (sim:정확도, date:최신순)
        String sort = request.getParameter("sort");
        if(sort == null || sort.isEmpty()) sort = "sim";
        
        // [추가] 페이지 번호 (값이 없거나 오류 시 기본 1페이지)
        int page = 1;
        try {
            String pageStr = request.getParameter("page");
            if(pageStr != null && !pageStr.isEmpty()) {
                page = Integer.parseInt(pageStr);
            }
        } catch(Exception e) { page = 1; }

        // [추가] 날짜 필터 옵션 (all, 1w, 1m, 1y, custom)
        String timeFilter = request.getParameter("timeFilter");
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        
        // 2. Naver API 호출
        String clientId = "네이버 클라우드 ID"; // 본인 값으로 변경
        String clientSecret = "네이버 2번째 ID값(뭐였는지 까먹음 2번째꺼 적으면됨)"; // 본인 값으로 변경
        String encodedKeyword = "";
        
        // 키워드가 null이 아닐 때만 인코딩
        if (keyword != null && !keyword.isEmpty()) {
            encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
        } else {
            // 키워드가 없으면 검색할 수 없음
            request.setAttribute("message", "검색어를 입력하세요.");
            RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
            dispatcher.forward(request, response);
            return;
        }

        // [변경] sort 파라미터 적용
        // URL 만들기: display=100으로 설정해 항상 최대치인 100개를 가져옵니다.
        // sort 변수를 넣어 사용자가 원한 정렬(최신순/정확도순)을 반영합니다.
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
            
            // 네이버가 보내준 응답(JSON)을 한 줄씩 읽어서 문자열(responseBody)로 만듭니다.
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

        // 3. Gson 라이브러리를 이용해 JSON -> 자바 객체(BlogResponse -> List<BlogItem>)로 변환
        List<BlogItem> items = null;
        try {
            Gson gson = new Gson();
            BlogResponse blogResponse = gson.fromJson(responseBody, BlogResponse.class);
            items = blogResponse.getItems();
            
         // [추가] HTML 태그(<b>) 제거 작업 (Gson으로 받은 직후 처리)
         // [태그 제거] 네이버 검색 결과는 검색어 강조를 위해 <b>맛집</b> 처럼 태그가 붙어옵니다.
         // 이를 깨끗하게 지우는 반복문입니다.
            if(items != null) {
                for(BlogItem item : items) {
                    item.setTitle(removeHtmlTag(item.getTitle()));
                    item.setDescription(removeHtmlTag(item.getDescription()));
                }
            }
        } catch (Exception e) {
             request.setAttribute("message", "JSON 파싱 실패: " + e.getMessage());
        }
        // 4. [핵심] 날짜 필터링 및 페이징 처리
        if (items != null) {
            // 4-1. 날짜 필터링 적용
        	// 사용자가 날짜 필터를 선택했다면 filterByDate 함수를 호출해 걸러냅니다.
            List<BlogItem> filteredItems = new ArrayList<>();
            if (timeFilter == null || timeFilter.equals("all") || timeFilter.isEmpty()) {
                filteredItems = items;
            } else {
            	// 필터가 있으면(예: 1주일), 날짜 지난 글은 버리고 남은 것만 filteredItems에 담습니다.
                filteredItems = filterByDate(items, timeFilter, startDate, endDate);
            }

            // 4-2. 페이징 (10개씩 자르기)
            int totalCount = filteredItems.size();
            int itemsPerPage = 10;
            // 1. 시작 번호 계산 (페이지당 10개일 때)
            // 1페이지면 0, 2페이지면 10, 3페이지면 20...
            int startIdx = (page - 1) * itemsPerPage;
            // 2. 끝 번호 계산 (핵심!)
            // 기본적으로는 '시작 + 10' 이지만, 전체 개수를 넘으면 에러가 나므로 Math.min을 씁니다.
            int endIdx = Math.min(startIdx + itemsPerPage, totalCount);

            List<BlogItem> pageItems = new ArrayList<>();
            // subList를 이용해 리스트를 자릅니다.(10개씩)
            // Math.min으로 끝 번호를 조정했으므로 startIdx가 totalCount보다 작을 때만 실행하여 에러 방지
            if (startIdx < totalCount) {
                pageItems = filteredItems.subList(startIdx, endIdx);
            }       

            // 5. 결과를 request에 담아서 search.jsp로 전달
            // (SaveServlet과 달리 DB 저장은 하지 않습니다)
            request.setAttribute("items", pageItems);// itmes를 pageItems로 변경 (잘라낸 10개만 전송)
            request.setAttribute("keyword", keyword); // 저장 버튼을 위해 키워드도 전달
        
            // 페이징/옵션 유지를 위한 값 전송
            request.setAttribute("currentPage", page); // 현재 몇 페이지인지
            request.setAttribute("totalCount", totalCount); // 총 몇 개인지
            request.setAttribute("sort", sort);
            request.setAttribute("timeFilter", timeFilter);
            request.setAttribute("startDate", startDate);
            request.setAttribute("endDate", endDate);
        
        } else {
        	request.setAttribute("items", null);
        }
        RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
        dispatcher.forward(request, response);
    }
    
    // ---------------------------------------------------------
    // [내부 메서드 1] 날짜 필터링 로직
    // ---------------------------------------------------------
    // 해당 메서드는 Calendar클래스를 사용해 날짜를 계산합니다.
    private List<BlogItem> filterByDate(List<BlogItem> items, String filter, String start, String end) {
        List<BlogItem> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); // 네이버 날짜 포맷
        
        Calendar cal = Calendar.getInstance();
        Date targetDate = null;
        Date customStart = null;
        Date customEnd = null;

        try {
            // 옵션에 따른 기준일 계산
            if(filter.equals("1w")) cal.add(Calendar.DATE, -7); // 1주일 전
            else if(filter.equals("1m")) cal.add(Calendar.MONTH, -1); // 1개월 전
            else if(filter.equals("1y")) cal.add(Calendar.YEAR, -1); // 1년 전
            
            // 사용자가 입력한 커스텀 날짜 범위 처리
            if(!filter.equals("custom")) { 
                targetDate = cal.getTime();
            } else if(start != null && end != null && !start.isEmpty() && !end.isEmpty()) {
                SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd");
                customStart = inputSdf.parse(start);
                customEnd = inputSdf.parse(end);
                // 종료일 23:59분까지 포함하기 위해 하루 더함
                Calendar c = Calendar.getInstance(); c.setTime(customEnd);
                c.add(Calendar.DATE, 1); customEnd = c.getTime();
            }
            // 위 시작일(start)과 종료일(end)을 작성일(postDate)과 비교하여 필터링
            for(BlogItem item : items) {
                Date postDate = sdf.parse(item.getPostdate());
                boolean isPass = false;

                if(filter.equals("custom")) {
                	//A.compareTo(B)를 실행하면, A가 B보다 큰지(미래), 작은지(과거), 같은지를 숫자로 알려줍니다.
                    if(customStart != null && customEnd != null) {
                        if(postDate.compareTo(customStart) >= 0 && postDate.compareTo(customEnd) < 0) isPass = true;
                    } else isPass = true;
                } else {
                    if(postDate.after(targetDate)) isPass = true;
                }

                if(isPass) result.add(item);
            }
        } catch(Exception e) { return items; } // 에러 시 필터링 안함
        return result;
    }
    
    // ---------------------------------------------------------
    // [내부 메서드 2] 태그 제거 유틸
    // ---------------------------------------------------------
    // 위 <b> 태그 제거의 메소드
    private String removeHtmlTag(String text) {
        if(text == null) return "";
        return text.replaceAll("<b>", "").replaceAll("</b>", "");
    }

	// index.jsp의 form method가 'get'이므로 doGet만 구현해도 되지만,
    // 혹시 모르니 doPost도 doGet을 호출하도록 추가
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        doGet(request, response);
    }

}
