package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SuggestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        String term = request.getParameter("term");
        if (term == null || term.trim().isEmpty()) {
            response.getWriter().write("[]");
            return;
        }

        // 1. 네이버의 실제 자동완성(연관검색어) API 주소 (비공개 API)
        // q=검색어, st=100(통합검색), r_format=json(결과형식)
        String encodedTerm = URLEncoder.encode(term, "UTF-8");
        String naverUrl = "https://ac.search.naver.com/nx/ac?q=" + encodedTerm + "&con=1&frm=nv&ans=2&r_format=json&st=100&kix=0&ari=1";

        try {
            // 2. Java가 네이버 서버에 접속 (Proxy 역할)
            URL url = new URL(naverUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            // 네이버인 척 하기 위해 User-Agent 설정 (선택사항)
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 3. 네이버가 준 응답 읽기
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            // 4. 읽은 내용을 그대로 우리 웹 브라우저에게 전달
            // (네이버의 JSON 형식을 그대로 Toss 합니다)
            response.getWriter().write(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("[]");
        }
    }
}