package sauer.logservice;

import com.google.appengine.api.log.AppLogLine;
import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogService;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MainServlet extends HttpServlet {

  private static final int MAX_RESULTS = 10;

  private static final DateFormat DATE_TIME_INSTANCE = new SimpleDateFormat(
      "dd/MM/yyyy:HH:mm:ss.SSS ZZZ");

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PrintWriter writer = resp.getWriter();
    resp.setContentType("text/html");

    writer.println("<!doctype html>");
    writer.println("<html>");
    writer.println("<head>");
    writer.println("<meta http-equiv='content-type' content='text/html; charset=UTF-8'>");
    writer.println("<title>sauer.logservice-java</title>");
    writer.println("</head>");
    writer.println("<body>");
    writer.println("<h1>sauer.logservice-java</h1>");

    long now = System.currentTimeMillis() * 1000;
    long then = now - 10 * 3600 * 1000 * 1000; // 10 minutes ago
    LogQuery query = LogQuery.Builder.withDefaults();

    //      String version = req.getParameter("version");
    //      List<String> versions = Arrays.asList(new String[] {version});
    //      query.majorVersionIds(versions);
    query.startTimeUsec(then).endTimeUsec(now);
    query.includeAppLogs(true);
    query.includeIncomplete(false);
    LogService logService = LogServiceFactory.getLogService();
    Iterable<RequestLogs> results = logService.fetch(query);

    writer.print("<pre>");
    int count = 0;
    for (RequestLogs log : results) {
      if (count++ == MAX_RESULTS) {
        break;
      }
      String startTimeString = DATE_TIME_INSTANCE.format(new Date(log.getEndTimeUsec() / 1000));

      String combined = log.getCombined();
      if ("".equals(combined)) {
        // temporary workaround for the Java dev_appserver
        combined = log.getIp() + " - " + log.getNickname() + " [" + startTimeString + "] \""
            + log.getMethod() + " " + log.getResource() + " " + log.getHttpVersion() + "\" "
            + log.getStatus() + " " + Math.max(0, log.getResponseSize()) + " - \""
            + log.getUserAgent() + "\"";
      }
      writer.println(combined);
      for (AppLogLine line : log.getAppLogLines()) {
        writer.print("\t" + line.getLogLevel().ordinal() + ":" + line.getTimeUsec() / 1000f + " ");
        writer.println(line.getLogMessage().replace("\n", "\n\t: "));
      }
    }
    writer.print("</pre>");

    writer.println("</body>");
    writer.println("</html>");
  }
}
