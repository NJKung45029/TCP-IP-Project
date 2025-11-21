<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="java.util.List, servlet.BlogItem" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Naver OpenAPI 검색</title>
<style>
    /* --- [기존 스타일] 레이아웃 및 연관검색어 --- */
    body { font-family: 'Malgun Gothic', sans-serif; padding: 20px; }
    .search-wrapper { display: inline-block; position: relative; }
    #keywordInput { width: 300px; padding: 8px; font-size: 14px; }
    #suggestion-box {
        display: none; position: absolute; top: 100%; left: 0;
        width: 300px; border: 1px solid #ccc; background-color: white;
        z-index: 9999; box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    }
    .suggestion-item { padding: 10px; cursor: pointer; font-size: 14px; color: #333; }
    .suggestion-item:hover { background-color: #f0f0f0; }

    /* --- [추가된 스타일] 결과 테이블 --- */
    .result-container { margin-top: 30px; border-top: 2px solid #eee; padding-top: 20px; }
    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
    th, td { border: 1px solid #ddd; padding: 10px; text-align: left; font-size: 14px; }
    th { background-color: #f9f9f9; }
    .save-btn {
        padding: 10px 20px; background-color: #03C75A; color: white;
        border: none; cursor: pointer; font-size: 16px; font-weight: bold;
    }
    .save-btn:hover { background-color: #029f48; }
    
    /* --- [추가] 옵션 버튼 및 패널 스타일 --- */
    .option-btn {
        background: #fff; border: 1px solid #ddd; padding: 8px 12px; 
        cursor: pointer; font-size: 13px; margin-left: 5px; border-radius: 4px;
        vertical-align: top; /* 인풋창과 높이 맞춤 */
    }
    .option-btn:hover { background: #f0f0f0; }
    
    #option-panel {
        display: none; /* 기본 숨김 */
        border: 1px solid #eee; background: #fafafa; 
        padding: 15px; margin-top: 10px; width: fit-content; border-radius: 8px;
    }
    .opt-group { margin-bottom: 10px; }
    .opt-group label { font-weight: bold; margin-right: 10px; font-size: 14px; }
    #custom-date-area { display: none; margin-left: 10px; } /* 날짜 직접입력창 숨김 */

    /* --- [추가] 페이징 버튼 스타일 --- */
    .pagination { text-align: center; margin-top: 20px; }
    .page-link {
        display: inline-block; padding: 6px 12px; border: 1px solid #ddd;
        margin: 0 2px; color: #333; text-decoration: none; cursor: pointer;
        border-radius: 3px;
    }
    .page-link.active { background-color: #03C75A; color: white; border-color: #03C75A; }
    .page-link:hover:not(.active) { background-color: #eee; }
    
</style>
</head>
<body>

    <h1>웹 서비스 개발 과제</h1>
    <h2>Naver OpenAPI 검색 (블로그)</h2>
    <!--id 추가-->
    <form action="search" method="get" autocomplete="off" id="searchForm">
    	    <!--페이지 표시-->
    	<input type="hidden" name="page" id="pageNum" value="1">
        
        검색어: 
        <div class="search-wrapper">
            <input type="text" name="keyword" id="keywordInput" 
                   placeholder="검색어를 입력하세요" onkeyup="fetchSuggestions()"
                   value="<%= (request.getAttribute("keyword") != null) ? request.getAttribute("keyword") : "" %>">
            
            <div id="suggestion-box"></div>
        </div>
        <input type="submit" value="검색">
        <!--옵션 버튼-->
        <button type="button" class="option-btn" onclick="toggleOptions()">옵션 ▼</button>

        <div id="option-panel">
            <div class="opt-group">
                <label>정렬:</label>
                <input type="radio" name="sort" value="sim" checked> 관련도순
                <input type="radio" name="sort" value="date"> 최신순
            </div>
            <div class="opt-group">
                <label>기간:</label>
                <select name="timeFilter" onchange="checkCustomDate(this)">
                    <option value="all">전체</option>
                    <option value="1w">1주일</option>
                    <option value="1m">1개월</option>
                    <option value="1y">1년</option>
                    <option value="custom">직접입력</option>
                </select>
                
                <span id="custom-date-area">
                    <input type="date" name="startDate"> ~ <input type="date" name="endDate">
                </span>
            </div>
            <button type="submit" style="padding: 5px 10px;">옵션 적용 검색</button>
        </div>
        
    </form>

    <%
        List<BlogItem> items = (List<BlogItem>) request.getAttribute("items");
        String message = (String) request.getAttribute("message");
        
     	// [추가] 페이지 계산 로직
        int currentPage = (request.getAttribute("currentPage") != null) ? (Integer)request.getAttribute("currentPage") : 1;
        int totalCount = (request.getAttribute("totalCount") != null) ? (Integer)request.getAttribute("totalCount") : 0;
        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil((double)totalCount / itemsPerPage);
        
        // 오류 메시지가 있으면 표시
        if (message != null) {
    %>
            <p style="color: red; margin-top: 20px;">오류: <%= message %></p>
    <%
        }

        // 검색 결과 리스트가 있으면 테이블 출력
        if (items != null && !items.isEmpty()) {
    %>
        <div class="result-container">
            <h3>검색 결과 (<%= items.size() %>건)</h3>
            
            <table>
            	<!--작성일 추가 링크 크기 지정-->
                <tr>
                	<th style="width: 100px;">작성일</th>
                    <th>제목</th>
                    <th>블로그명</th>
                    <th>내용(일부)</th>
                    <th style="width: 60px;">링크</th>
                </tr>
                <%
                    for (BlogItem item : items) {
                        String desc = item.getDescription();
                        // 내용이 너무 길면 자르기
                        String shortDesc = (desc != null && desc.length() > 50) ? desc.substring(0, 50) + "..." : desc;
                %>
                <tr>
                	<!--사용자 지정 시간검색정렬 추가-->
                	<td><%= item.getPostdate() %></td>
                    <td><%= item.getTitle() %></td>
                    <td><%= item.getBloggername() %></td>
                    <td><%= shortDesc %></td>
                    <td><a href="<%= item.getLink() %>" target="_blank">이동</a></td>
                </tr>
                <%
                    }
                %>
            </table>

            <form action="save" method="post">
                <input type="hidden" name="keyword" value="<%= request.getAttribute("keyword") %>">
                <!-- 사용자가 최신순으로 보고 있었을 때에도 최신순 100개가 저장 -->
                <input type="hidden" name="sort" value="<%= request.getAttribute("sort") %>">
                
                <input type="submit" value="이 결과 100건 DB에 저장하기" class="save-btn">
            </form>
            <!-- 페이지 표시 추가 -->
            <div class="pagination"> 
                <% if(totalPages > 0) { %>
                    <%-- 이전 버튼 --%>
                    <% if(currentPage > 1) { %>
                        <a class="page-link" onclick="movePage(<%= currentPage - 1 %>)">이전</a>
                    <% } %>

                    <%-- 페이지 번호 --%>
                    <% for(int i=1; i<=totalPages; i++) { %>
                        <a class="page-link <%= (i==currentPage) ? "active" : "" %>" 
                           onclick="movePage(<%= i %>)"><%= i %></a>
                    <% } %>

                    <%-- 다음 버튼 --%>
                    <% if(currentPage < totalPages) { %>
                        <a class="page-link" onclick="movePage(<%= currentPage + 1 %>)">다음</a>
                    <% } %>
                <% } %>
            </div>
        </div>
    <%
        } else if (request.getAttribute("keyword") != null && items == null) {
            // 검색어는 입력했는데 결과가 없는 경우 (서블릿을 다녀왔는데 items가 null)
    %>
            <p style="margin-top: 20px;">검색 결과가 없습니다.</p>
    <%
        }
    %>

    <script>
	 // --- [기존 기능] 연관 검색어 ---
        function fetchSuggestions() {
            const input = document.getElementById("keywordInput");
            const box = document.getElementById("suggestion-box");
            const term = input.value;

            if (term.trim().length === 0) {
                box.style.display = "none";
                return;
            }

            fetch('./suggest?term=' + encodeURIComponent(term))
                .then(res => res.json())
                .then(data => {
                    box.innerHTML = "";
                    if (data && data.items && data.items.length > 0 && data.items[0].length > 0) {
                        data.items[0].forEach(item => {
                            const div = document.createElement("div");
                            div.className = "suggestion-item";
                            div.textContent = item[0];
                            div.onclick = function() {
                                input.value = item[0];
                                box.style.display = "none";
                            };
                            box.appendChild(div);
                        });
                        box.style.display = "block";
                    } else {
                        box.style.display = "none";
                    }
                })
                .catch(err => { box.style.display = "none"; });
        }

        document.addEventListener('click', function(e) {
            const wrapper = document.querySelector('.search-wrapper');
            if (!wrapper.contains(e.target)) {
                document.getElementById("suggestion-box").style.display = 'none';
            }
        });
        
// --- [추가 기능] 옵션 및 페이징 관련 스크립트 ---
        
        // 1. 옵션 패널 열기/닫기 토글
        function toggleOptions() {
            var panel = document.getElementById("option-panel");
            if (panel.style.display === "none" || panel.style.display === "") {
                panel.style.display = "block";
            } else {
                panel.style.display = "none";
            }
        }

        // 2. '직접입력' 선택 시에만 날짜 입력칸 보이기
        function checkCustomDate(select) {
            var customArea = document.getElementById("custom-date-area");
            if (select.value === "custom") {
                customArea.style.display = "inline";
            } else {
                customArea.style.display = "none";
            }
        }

        // 3. 페이지 이동 함수 (form을 submit하여 검색어와 옵션 유지)
        function movePage(pageNum) {
            document.getElementById("pageNum").value = pageNum;
            document.getElementById("searchForm").submit();
        }

        // 4. [중요] 페이지 로드 시 기존 선택값(정렬, 기간 등) 복구하기
        window.onload = function() {
            var savedSort = "<%= request.getAttribute("sort") %>";
            var savedTime = "<%= request.getAttribute("timeFilter") %>";
            
            // 정렬 복구 (sim 또는 date 체크)
            if(savedSort && savedSort !== "null") {
                var sortRadio = document.querySelector('input[name="sort"][value="'+savedSort+'"]');
                if(sortRadio) sortRadio.checked = true;
            }
            
            // 기간 복구
            if(savedTime && savedTime !== "null") {
                var timeSelect = document.querySelector('select[name="timeFilter"]');
                if(timeSelect) timeSelect.value = savedTime;
                
                // 직접입력이면 날짜칸 보여주기
                if(savedTime === "custom") {
                    document.getElementById("custom-date-area").style.display = "inline";
                }
                
                // 옵션을 건드렸으면 패널을 열어두기
                if(savedSort !== "sim" || savedTime !== "all") {
                    document.getElementById("option-panel").style.display = "block";
                }
            }
            
            // 날짜 복구
            var start = "<%= request.getAttribute("startDate") %>";
            var end = "<%= request.getAttribute("endDate") %>";
            if(start && start !== "null") document.getElementsByName("startDate")[0].value = start;
            if(end && end !== "null") document.getElementsByName("endDate")[0].value = end;
        };
    </script>

</body>
</html>