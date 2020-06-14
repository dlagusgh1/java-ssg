// 명령어 도움말
// member 기능
// 1. member login : 로그인
// 2. member logout : 로그아웃
// 3. member join : 회원가입
// 4. member whoami : 로그인 한 대상 이름 확인
//
// article 기능
// 1. article write 게시판 번호: 게시글 작성(자유/공지 구분)
// 2. article delete 게시물 번호 : 게시물 삭제 기능
// 3. article modify 게시물 번호 : 게시물 수정 기능
// 4. article list : 게시물 리스팅 기능(구분없이)
// 4-1. article list 게시판 번호 : 미 구현
// 5. article detail 게시물 번호 : 미 구현
//
// site 기능
// 1. build site : html 파일 생성(수동)
// 2. build startAutoSite : html 파일 생성(자동)
// 3. build stopAutoSite : 미 구현(만들기는 했으나, 입력해도 멈추지 않음.)

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class Main {
	public static void main(String[] args) {
		App app = new App();
		app.start();
	}
}

// Session
// 현재 사용자가 이용중인 정보
// 이 안의 정보는 사용자가 프로그램을 사용할 때 동안은 계속 유지된다.
class Session {
	private Member loginedMember;
	private Board currentBoard;

	public Member getLoginedMember() {
		return loginedMember;
	}

	public void setLoginedMember(Member loginedMember) {
		this.loginedMember = loginedMember;
	}

	public Board getCurrentBoard() {
		return currentBoard;
	}

	public void setCurrentBoard(Board currentBoard) {
		this.currentBoard = currentBoard;
	}

	public boolean isLogined() {
		return loginedMember != null;
	}
}

// Factory
// 프로그램 전체에서 공유되는 객체 리모콘을 보관하는 클래스
class Factory {
	private static Session session;
	private static DB db;
	private static BuildService buildService;
	private static ArticleService articleService;
	private static ArticleDao articleDao;
	private static MemberService memberService;
	private static MemberDao memberDao;
	private static Scanner scanner;

	public static Session getSession() {
		if (session == null) {
			session = new Session();
		}

		return session;
	}

	public static Scanner getScanner() {
		if (scanner == null) {
			scanner = new Scanner(System.in);
		}

		return scanner;
	}

	public static DB getDB() {
		if (db == null) {
			db = new DB();
		}

		return db;
	}

	public static ArticleService getArticleService() {
		if (articleService == null) {
			articleService = new ArticleService();
		}

		return articleService;
	}

	public static ArticleDao getArticleDao() {
		if (articleDao == null) {
			articleDao = new ArticleDao();
		}

		return articleDao;
	}

	public static MemberService getMemberService() {
		if (memberService == null) {
			memberService = new MemberService();
		}
		return memberService;
	}

	public static MemberDao getMemberDao() {
		if (memberDao == null) {
			memberDao = new MemberDao();
		}

		return memberDao;
	}

	public static BuildService getBuildService() {
		if (buildService == null) {
			buildService = new BuildService();
		}

		return buildService;
	}
}

// App
class App {
	private Map<String, Controller> controllers;

	// 컨트롤러 만들고 한곳에 정리
// 나중에 컨트롤러 이름으로 쉽게 찾아쓸 수 있게 하려고 Map 사용
	void initControllers() {
		controllers = new HashMap<>();
		controllers.put("build", new BuildController());
		controllers.put("article", new ArticleController());
		controllers.put("member", new MemberController());
	}

	public App() {
		// 컨트롤러 등록
		initControllers();
		// 관리자 회원 생성
		Factory.getMemberService().join("admin", "admin", "관리자");

		// 공지사항 게시판 생성
		Factory.getArticleService().makeBoard("공지시항", "notice");
		// 자유 게시판 생성
		Factory.getArticleService().makeBoard("자유게시판", "free");

		// 현재 게시판을 1번 게시판으로 선택
		Factory.getSession().setCurrentBoard(Factory.getArticleService().getBoard(1));
		// 임시 : 현재 로그인 된 회원은 1번 회원으로 지정, 이건 나중에 회원가입, 로그인 추가되면 제거해야함
		Factory.getSession().setLoginedMember(Factory.getMemberService().getMember(1));
	}

	public void start() {

		while (true) {
			System.out.printf("명령어 : ");
			String command = Factory.getScanner().nextLine().trim();

			// 입력된 커맨트가 없는 경우 재 입력 요청
			if (command.length() == 0) {
				continue;
				// exit 입력 시 게시판 종료
			} else if (command.equals("exit")) {
				break;
			}

			Request reqeust = new Request(command);

			if (reqeust.isValidRequest() == false) {
				continue;
			}

			if (controllers.containsKey(reqeust.getControllerName()) == false) {
				continue;
			}

			controllers.get(reqeust.getControllerName()).doAction(reqeust);
		}

		Factory.getScanner().close();
	}
}

// Request
class Request {
	private String requestStr;
	private String controllerName;
	private String actionName;
	private String arg1;
	private String arg2;
	private String arg3;

	boolean isValidRequest() {
		return actionName != null;
	}

	Request(String requestStr) {
		this.requestStr = requestStr;
		String[] requestStrBits = requestStr.split(" ");
		this.controllerName = requestStrBits[0];

		if (requestStrBits.length > 1) {
			this.actionName = requestStrBits[1];
		}

		if (requestStrBits.length > 2) {
			this.arg1 = requestStrBits[2];
		}

		if (requestStrBits.length > 3) {
			this.arg2 = requestStrBits[3];
		}

		if (requestStrBits.length > 4) {
			this.arg3 = requestStrBits[4];
		}
	}

	public String getControllerName() {
		return controllerName;
	}

	public void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getArg2() {
		return arg2;
	}

	public void setArg2(String arg2) {
		this.arg2 = arg2;
	}

	public String getArg3() {
		return arg3;
	}

	public void setArg3(String arg3) {
		this.arg3 = arg3;
	}
}

// Controller 
abstract class Controller {
	abstract void doAction(Request reqeust);
}

class ArticleController extends Controller {
	private ArticleService articleService;

	ArticleController() {
		articleService = Factory.getArticleService();
	}

	public void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("list")) {
			actionList(reqeust);
		} else if (reqeust.getActionName().equals("write")) {
			actionWrite(reqeust, reqeust.getArg1());
		} else if (reqeust.getActionName().equals("modify")) {
			actionModify(reqeust, reqeust.getArg1());
		} else if (reqeust.getActionName().equals("delete")) {
			actionDelete(reqeust, reqeust.getArg1());
		}
	}

	private void actionDelete(Request reqeust, String arg1) {

		if (Factory.getSession().getLoginedMember() == null) {
			System.out.println("로그인 한 회원만 가능합니다.");
		} else {
			String filePath = "db\\article\\" + arg1 + ".json";

			if (Util.isFileExists(filePath)) {
				articleService.articleDelete(filePath);
			} else {
				System.out.println("해당 파일이 존재하지 않습니다.");
			}
		}
	}

	private void actionList(Request reqeust) {
		List<Article> articles = articleService.getArticles();
		if (articles.size() != 0) {
			System.out.println("번호 | 제목 | 작성 날짜");
			for (int i = 0; i < articles.size(); i++) {

				Article a = articles.get(i);
				System.out.printf("%d | %s | %s \n", a.getId(), a.getTitle(), a.getRegDate());
			}
		} else {
			System.out.println("게시물이 존재하지 않습니다.");
		}

	}

	private void actionModify(Request reqeust, String arg1) {

		if (Factory.getSession().getLoginedMember() == null) {
			System.out.println("로그인 한 회원만 가능합니다.");
		} else {
			String filePath = "db\\article\\" + arg1 + ".json";

			if (Util.isFileExists(filePath)) {

				int id = Integer.parseInt(arg1);

				Article article = Factory.getArticleService().getArticlebyId(id);

				System.out.println("수정 할 제목 : ");
				String title = Factory.getScanner().nextLine();

				System.out.printf("수정 할 내용 : ");
				String body = Factory.getScanner().nextLine();

				articleService.modify(title, body, id);

				System.out.printf("%d번 글이 수정되었습니다.\n", id);

			} else {
				System.out.println("수정할 파일이 존재하지 않습니다.");
			}
		}
	}

	private void actionWrite(Request reqeust, String arg1) {

		String title;
		String body;
		int boardId;
		int memberId;
		int newId;

		if (Factory.getSession().getLoginedMember() == null) {
			System.out.println("로그인 한 회원만 가능합니다.");
		} else {
			if (arg1.equals("1")) {
				if (Factory.getSession().getLoginedMember().getName().equals("관리자")) {
					System.out.printf("제목 : ");
					title = Factory.getScanner().nextLine();
					System.out.printf("내용 : ");
					body = Factory.getScanner().nextLine();

					// 현재 게시판 id 가져오기
					boardId = Integer.parseInt(arg1);

					// 현재 로그인한 회원의 id 가져오기
					memberId = Factory.getSession().getLoginedMember().getId();
					newId = articleService.write(boardId, memberId, title, body);

					System.out.printf("%d번 글이 생성되었습니다.\n", newId);
				} else {
					System.out.println("공지사항은 관리자만 작성 가능합니다.");
				}
			}
			if (arg1.equals("2")) {
				System.out.printf("제목 : ");
				title = Factory.getScanner().nextLine();
				System.out.printf("내용 : ");
				body = Factory.getScanner().nextLine();

				// 현재 게시판 id 가져오기
				boardId = Integer.parseInt(arg1);

				// 현재 로그인한 회원의 id 가져오기
				memberId = Factory.getSession().getLoginedMember().getId();
				newId = articleService.write(boardId, memberId, title, body);

				System.out.printf("%d번 글이 생성되었습니다.\n", newId);
			}
		}

	}
}

// 사이트 관련 컨트롤러(html 생성)
class BuildController extends Controller {
	private BuildService buildService;
	boolean workstarted;

	BuildController() {
		buildService = Factory.getBuildService();
	}

	@Override
	void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("site")) {
			actionSite(reqeust);
			actionCreatMain();
			actionCreatLogin();
		} else if (reqeust.getActionName().equals("startAutoSite")) {
			actionAutoSite(true);
		} else if (reqeust.getActionName().equals("stopAutoSite")) {
			actionAutoSite(false);
		}
	}

	void actionAutoSite(boolean workstarted) {
		if (workstarted == true) {
			System.out.println("AutoSite 기능이 실행 됩니다.");
			Util.startAutoSite(workstarted);
		} else if (workstarted != true) {
			System.out.println("AutoSite 기능이 종료 됩니다.");
			Util.startAutoSite(workstarted);
		}
	}

	private void actionCreatLogin() {
		buildService.creatLogin();
	}

	private void actionCreatMain() {
		buildService.creatMain();
	}

	private void actionSite(Request reqeust) {
		buildService.buildSite();
	}
}

// 맴버 관련 컨트롤러
class MemberController extends Controller {
	private MemberService memberService;

	MemberController() {
		memberService = Factory.getMemberService();
	}

	// 커맨드 입력받은 것 중 맴버 기능관련 입력 처리(전달)
	void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("logout")) {
			actionLogout(reqeust);
		} else if (reqeust.getActionName().equals("login")) {
			actionLogin(reqeust);
		} else if (reqeust.getActionName().equals("whoami")) {
			actionWhoami(reqeust);
		} else if (reqeust.getActionName().equals("join")) {
			actionJoin(reqeust);
		}
	}

// 회원 가입 관련
	private void actionJoin(Request reqeust) {
		while (true) {
			System.out.printf("회원가입 아이디 : ");
			String loginId = Factory.getScanner().nextLine().trim();

			if (loginId.equals("admin")) {
				System.out.println("해당 아이디로는 생성이 불가능합니다.");
				continue;
			}

			System.out.printf("회원가입 비번 : ");
			String loginPw = Factory.getScanner().nextLine().trim();

			System.out.printf("회원가입 이름 : ");
			String name = Factory.getScanner().nextLine().trim();

			if (name.equals("관리자")) {
				System.out.println("해당 이름으로는 생성이 불가능합니다.");
				continue;
			}

			if (memberService.join(loginId, loginPw, name) == -1) {
				System.out.println("중복된 아이디가 존재합니다.");
			} else {
				System.out.println("회원가입이 완료되었습니다.");
				break;
			}
		}
	}

// 로그인 중인 대상이 누구인지 확인(완료)
	private void actionWhoami(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember == null) {
			System.out.println("현재 로그인한 대상이 없습니다.");
		} else {
			System.out.println(loginedMember.getName());
		}

	}

// 로그인 기능
	private void actionLogin(Request reqeust) {
		// 중복 로그인 방지 기능
		if (Factory.getSession().getLoginedMember() != null) {
			System.out.println("다른 대상이 로그인 중 입니다.");
		} else {
			System.out.printf("로그인 아이디 : ");
			String loginId = Factory.getScanner().nextLine().trim();

			System.out.printf("로그인 비번 : ");
			String loginPw = Factory.getScanner().nextLine().trim();

			Member member = memberService.getMemberByLoginIdAndLoginPw(loginId, loginPw);

			if (member == null) {
				System.out.println("일치하는 회원이 없습니다.");
			} else {
				System.out.println(member.getName() + "님 환영합니다.");
				Factory.getSession().setLoginedMember(member);
			}
		}
	}

// 로그아웃 기능(로그아웃 시 loginedMember 값이 null로 변경)
	private void actionLogout(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			Session session = Factory.getSession();
			System.out.println("로그아웃 되었습니다.");
			session.setLoginedMember(null);
		}

	}
}

// Servicee
class BuildService {
	ArticleService articleService;

	BuildService() {
		articleService = Factory.getArticleService();
	}

	public void creatLogin() {
		Util.makeDir("site");
		Util.makeDir("site/article");
		String head = Util.getFileContents("site_template/part/head.html");
		String foot = Util.getFileContents("site_template/part/foot.html");

		String fileName = "login.html";

		String html = "";

		String template = Util.getFileContents("site_template/article/login.html");

		html = template.replace("${TR}", html);

		html = head + html + foot;

		Util.writeFileContents("site/article/" + fileName, html);
	}

	public void creatMain() {
		Util.makeDir("site");
		Util.makeDir("site/article");
		String head = Util.getFileContents("site_template/part/head.html");
		String foot = Util.getFileContents("site_template/part/foot.html");

		String fileName = "main.html";

		String html = "";

		String template = Util.getFileContents("site_template/article/main.html");

		html += "<style> .main-box {text-align: center; padding: 100px;}";
		html += ".main-box > img{width: 800px; border-radius: 50px;}</style>";

		html += "<nav class=\"main-box\">";
		html += "<img src=\"main\\images\\main1.jpg\" alt=\"\"></nav>";

		html = head + html + foot;

		Util.writeFileContents("site/article/" + fileName, html);

	}

	// bulid site 명령어를 통해 html 생성.d
	public void buildSite() {

		Util.makeDir("site");
		Util.makeDir("site/article");

		String head = Util.getFileContents("site_template/part/head.html");
		String foot = Util.getFileContents("site_template/part/foot.html");

		// 전체 게시판 가져오기.
		List<Board> boards = articleService.getBoards();

		// 각 게시판 별 게시물 리스트 페이지 생성
		for (Board board : boards) {
			String fileName = board.getCode() + "-list-1.html";

			String html = "";

			List<Article> articles = articleService.getArticlesByBoardCode(board.getCode());

			String template = Util.getFileContents("site_template/article/list.html");

			System.out.println("boardCode : " + board.getCode() + "생성 완료");

			if (board.getCode().equals("notice")) {
				html += "<h2>공지사항</h2>";
			} else if (board.getCode().equals("free")) {
				html += "<h2>자유 게시판</h2>";
			}
			for (Article article : articles) {

				html += "<tr>";
				html += "<td>" + article.getId() + "</td>";
				html += "<td><a href=\"" + article.getId() + ".html\">" + article.getTitle() + "</a></td>";
				html += "<td>" + Factory.getSession().getLoginedMember().getName() + "</td>";
				html += "<td>" + article.getRegDate() + "</td>";
				html += "</tr>";
			}

			html = template.replace("${TR}", html);

			html = head + html + foot;

			Util.writeFileContents("site/article/" + fileName, html);
		}

		// 게시물 별 파일 생성
		List<Article> articles = articleService.getArticles();

		for (Article article : articles) {

			String template = Util.getFileContents("site_template/article/detail.html");

			String html = "";

			html += "<h2 class=\"t1-h\">상세보기</h2>";
			html += "<table border=1>";
			html += "<thead>";
			html += "<tr>";
			html += "<td class=\"td1\" colspan=4>게시물 상세보기</td>";
			html += "</tr>";
			html += "<tr>";
			html += "<td class=\"td1\">제목</td>";
			html += "<td colspan=3>" + article.getTitle() + "</td>";
			html += "</tr>";
			html += "</tr>";
			html += "<td class=\"td1\">내용</td>";
			html += "<td colspan=3>" + article.getBody() + "</td>";
			html += "</tr>";
			html += "<tr>";
			html += "<td class=\"td1\"><a href=\"" + (article.getId() - 1) + ".html\">이전글</a></td>";
			html += "<td class=\"td1\"><a href=\"" + (article.getId() + 1) + ".html\">다음글</a></td>";
			html += "</tr>";

			html = template.replace("${TR}", html);

			html = head + html + foot;

			Util.writeFileContents("site/article/" + article.getId() + ".html", html);

		}

		System.out.println("파일 생성이 완료되었습니다.");

	}

}

class ArticleService {
	private ArticleDao articleDao;

	ArticleService() {
		articleDao = Factory.getArticleDao();
	}

	public List<Article> getArticlesByBoardCode(String code) {
		return articleDao.getArticlesByBoardCode(code);

	}

	public List<Board> getBoards() {
		return articleDao.getBoards();
	}

	public void articleDelete(String filePath) {
		articleDao.articleDelete(filePath);
	}

	public void modify(String title, String body, int id) {
		articleDao.modify(title, body, id);
	}

	public Article getArticlebyId(int id) {
		return articleDao.getArticlebyId(id);
	}

	public int makeBoard(String name, String code) {
		Board oldBoard = articleDao.getBoardByCode(code);

		if (oldBoard != null) {
			return -1;
		}

		Board board = new Board(name, code);
		return articleDao.saveBoard(board);
	}

	public Board getBoard(int id) {
		return articleDao.getBoard(id);
	}

	public int write(int boardId, int memberId, String title, String body) {
		Article article = new Article(boardId, memberId, title, body);
		return articleDao.save(article);
	}

	public List<Article> getArticles() {
		return articleDao.getArticles();
	}

}

class MemberService {
	private MemberDao memberDao;

	MemberService() {
		memberDao = Factory.getMemberDao();
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		return memberDao.getMemberByLoginIdAndLoginPw(loginId, loginPw);
	}

	public int join(String loginId, String loginPw, String name) {
		Member oldMember = memberDao.getMemberByLoginId(loginId);

		if (oldMember != null) {
			return -1;
		}

		Member member = new Member(loginId, loginPw, name);
		return memberDao.save(member);
	}

	public Member getMember(int id) {
		return memberDao.getMember(id);
	}
}

// Dao
class ArticleDao {
	DB db;

	ArticleDao() {
		db = Factory.getDB();
	}

	public List<Article> getArticlesByBoardCode(String code) {
		return db.getArticlesByBoardCode(code);
	}

	public List<Board> getBoards() {
		return db.getBoards();
	}

	public void articleDelete(String filePath) {
		db.articleDelete(filePath);
	}

	public void modify(String title, String body, int id) {
		Article article = getArticlebyId(id);
		db.modify(title, body, article);
	}

	public Article getArticlebyId(int id) {
		return db.getArticlebyId(id);
	}

	public Board getBoardByCode(String code) {
		return db.getBoardByCode(code);
	}

	public int saveBoard(Board board) {
		return db.saveBoard(board);
	}

	public int save(Article article) {
		return db.saveArticle(article);
	}

	public Board getBoard(int id) {
		return db.getBoard(id);
	}

	public List<Article> getArticles() {
		return db.getArticles();
	}

}

class MemberDao {
	DB db;

	MemberDao() {
		db = Factory.getDB();
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		return db.getMemberByLoginIdAndLoginPw(loginId, loginPw);
	}

	public Member getMemberByLoginId(String loginId) {
		return db.getMemberByLoginId(loginId);
	}

	public Member getMember(int id) {
		return db.getMember(id);
	}

	public int save(Member member) {
		return db.saveMember(member);
	}
}

// DB
class DB {
	private Map<String, Table> tables;

	public DB() {
		String dbDirPath = getDirPath();
		Util.makeDir(dbDirPath);

		tables = new HashMap<>();

		tables.put("article", new Table<Article>(Article.class, dbDirPath));
		tables.put("board", new Table<Board>(Board.class, dbDirPath));
		tables.put("member", new Table<Member>(Member.class, dbDirPath));
	}

	public List<Article> getArticlesByBoardCode(String code) {

		// Board 코드 free 2 / notice 1
		Board board = getBoardByCode(code);

		// articles 구분없이 전부 articles에 넣어져 있다.
		List<Article> articles = getArticles();

		// 구분을 위한 빈 공간
		List<Article> newArticles = new ArrayList<>();

		// Board 코드와 일치 하는 것 담고
		for (Article article : articles) {
			if (article.getBoardId() == board.getId()) {
				newArticles.add(article);
			}
		}

		return newArticles;
	}

	public void articleDelete(String filePath) {
		tables.get("article").delete(filePath);

	}

	public void modify(String title, String body, Article article) {
		tables.get("article").modify(title, body, article);
	}

	public Article getArticlebyId(int id) {

		List<Article> articles = getArticles();

		for (Article article : articles) {
			if (article.getId() == id) {
				return article;
			}
		}

		return null;
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId) && member.getLoginPw().equals(loginPw)) {
				return member;
			}
		}

		return null;
	}

	public Member getMemberByLoginId(String loginId) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId)) {
				return member;
			}
		}

		return null;
	}

	public List<Member> getMembers() {
		return tables.get("member").getRows();
	}

	public Board getBoardByCode(String code) {
		List<Board> boards = getBoards();

		for (Board board : boards) {
			if (board.getCode().equals(code)) {
				return board;
			}
		}

		return null;
	}

	public List<Board> getBoards() {
		return tables.get("board").getRows();
	}

	public Member getMember(int id) {
		return (Member) tables.get("member").getRow(id);
	}

	public int saveBoard(Board board) {
		return tables.get("board").saveRow(board);
	}

	public String getDirPath() {
		return "db";
	}

	public int saveMember(Member member) {
		return tables.get("member").saveRow(member);
	}

	public Board getBoard(int id) {
		return (Board) tables.get("board").getRow(id);
	}

	public List<Article> getArticles() {
		return tables.get("article").getRows();
	}

	public int saveArticle(Article article) {
		return tables.get("article").saveRow(article);
	}

	public void backup() {
		for (String tableName : tables.keySet()) {
			Table table = tables.get(tableName);
			table.backup();
		}
	}
}

// Table
class Table<T> {
	private Class<T> dataCls;
	private String tableName;
	private String tableDirPath;

	public Table(Class<T> dataCls, String dbDirPath) {
		this.dataCls = dataCls;
		this.tableName = Util.lcfirst(dataCls.getCanonicalName());
		this.tableDirPath = dbDirPath + "/" + this.tableName;

		Util.makeDir(tableDirPath);
	}

	public void modify(String title, String body, Article article) {
		Dto dto = (Dto) article;
		T data = (T) article;

		String FilePath = getRowFilePath(dto.getId());
		File file = new File(FilePath);

		Util.deleteFile(FilePath);

		article.setTitle(title);
		article.setBody(body);
		data = (T) article;

		String rowFilePath = getRowFilePath(dto.getId());

		Util.writeJsonFile(rowFilePath, data);
	}

	private String getTableName() {
		return tableName;
	}

	public int saveRow(T data) {
		Dto dto = (Dto) data;

		if (dto.getId() == 0) {
			int lastId = getLastId();
			int newId = lastId + 1;
			dto.setId(newId);
			setLastId(newId);
		}

		String rowFilePath = getRowFilePath(dto.getId());

		Util.writeJsonFile(rowFilePath, data);

		return dto.getId();
	};

	private String getRowFilePath(int id) {
		return tableDirPath + "/" + id + ".json";
	}

	private void setLastId(int lastId) {
		String filePath = getLastIdFilePath();
		Util.writeFileContents(filePath, lastId);
	}

	private int getLastId() {
		String filePath = getLastIdFilePath();

		if (Util.isFileExists(filePath) == false) {
			int lastId = 0;
			Util.writeFileContents(filePath, lastId);
			return lastId;
		}

		return Integer.parseInt(Util.getFileContents(filePath));
	}

	private String getLastIdFilePath() {
		return this.tableDirPath + "/lastId.txt";
	}

	public T getRow(int id) {
		return (T) Util.getObjectFromJson(getRowFilePath(id), dataCls);
	}

	public void backup() {

	}

	void delete(String filePath) {
		Util.deleteFile(filePath);
	};

	List<T> getRows() {
		int lastId = getLastId();

		List<T> rows = new ArrayList<>();

		for (int id = 1; id <= lastId; id++) {
			T row = getRow(id);

			if (row != null) {
				rows.add(row);
			}
		}

		return rows;
	};
}

// DTO
abstract class Dto {
	private int id;
	private String regDate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRegDate() {
		return regDate;
	}

	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}

	Dto() {
		this(0);
	}

	Dto(int id) {
		this(id, Util.getNowDateStr());
	}

	Dto(int id, String regDate) {
		this.id = id;
		this.regDate = regDate;
	}
}

class Board extends Dto {
	private String name;
	private String code;

	public Board() {
	}

	public Board(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}

class Article extends Dto {
	private int boardId;
	private int memberId;
	private String title;
	private String body;

	public Article() {

	}

	public Article(int boardId, int memberId, String title, String body) {
		this.boardId = boardId;
		this.memberId = memberId;
		this.title = title;
		this.body = body;
	}

	public int getBoardId() {
		return boardId;
	}

	public void setBoardId(int boardId) {
		this.boardId = boardId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "Article [boardId=" + boardId + ", memberId=" + memberId + ", title=" + title + ", body=" + body
				+ ", getBoardId()=" + getBoardId() + ", getMemberId()=" + getMemberId() + "]";
	}

}

class ArticleReply extends Dto {
	private int id;
	private String regDate;
	private int articleId;
	private int memberId;
	private String body;

	ArticleReply() {

	}

	public int getArticleId() {
		return articleId;
	}

	public void setArticleId(int articleId) {
		this.articleId = articleId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}

class Member extends Dto {
	private String loginId;
	private String loginPw;
	private String name;

	public Member() {

	}

	public Member(String loginId, String loginPw, String name) {
		this.loginId = loginId;
		this.loginPw = loginPw;
		this.name = name;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getLoginPw() {
		return loginPw;
	}

	public void setLoginPw(String loginPw) {
		this.loginPw = loginPw;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

// Util
class Util {

	// 파일 삭제
	public static void deleteFile(String filePath) {
		File file = new File(filePath);
		if (file.exists()) {
			if (file.delete()) {
				System.out.println("삭제가 완료되었습니다.");
			} else {
				System.out.println("삭제에 실패하였습니다.");
			}
		} else {
			System.out.println("파일이 존재하지 않습니다.");
		}
	}

// 사이트 자동생성
	public static void startAutoSite(boolean workstarted) {
		new Thread(() -> {
			while (workstarted) {
				System.out.println(workstarted);
				if (workstarted) {
					try {
						System.out.println("AutoSite 실행 중");
						Factory.getBuildService().buildSite();
						Thread.sleep(10000); // 10초 딜레이
					} catch (InterruptedException e) {
					}
				} else if (workstarted == false) {
					System.out.println("AutoSite 종료");
					System.out.println(workstarted);
					break;
				}
			}
		}).start();
	}

// 현재날짜문장
	public static String getNowDateStr() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat Date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = Date.format(cal.getTime());
		return dateStr;
	}

// 파일에 내용쓰기 ( 파일 안에 내용을 적는 것 )
	public static void writeFileContents(String filePath, int data) {
		writeFileContents(filePath, data + "");
	}

// 첫 문자 소문자화
	public static String lcfirst(String str) {
		String newStr = "";
		newStr += str.charAt(0);
		newStr = newStr.toLowerCase();

		return newStr + str.substring(1);
	}

// 파일이 존재하는지
	public static boolean isFileExists(String filePath) {
		File f = new File(filePath);
		if (f.isFile()) {
			return true;
		}

		return false;
	}

// 파일내용 읽어오기
	public static String getFileContents(String filePath) {
		String rs = null;
		try {
			// 바이트 단위로 파일읽기
			FileInputStream fileStream = null; // 파일 스트림

			fileStream = new FileInputStream(filePath);// 파일 스트림 생성
			// 버퍼 선언
			byte[] readBuffer = new byte[fileStream.available()];
			while (fileStream.read(readBuffer) != -1) {
			}

			rs = new String(readBuffer);

			fileStream.close(); // 스트림 닫기
		} catch (Exception e) {
			e.getStackTrace();
		}

		return rs;
	}

// 파일 쓰기
	public static void writeFileContents(String filePath, String contents) {
		BufferedOutputStream bs = null;
		try {
			bs = new BufferedOutputStream(new FileOutputStream(filePath));
			bs.write(contents.getBytes()); // Byte형으로만 넣을 수 있음
		} catch (Exception e) {
			e.getStackTrace();
		} finally {
			try {
				bs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

// Json안에 있는 내용을 가져오기
	public static Object getObjectFromJson(String filePath, Class cls) {
		ObjectMapper om = new ObjectMapper();
		Object obj = null;
		try {
			obj = om.readValue(new File(filePath), cls);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {
			e.printStackTrace();
		}

		return obj;
	}

	public static void writeJsonFile(String filePath, Object obj) {
		ObjectMapper om = new ObjectMapper();
		try {
			om.writeValue(new File(filePath), obj);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void makeDir(String dirPath) {
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
}