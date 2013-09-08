package com.bestbuy.eos.alerts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class QuartzJob extends QuartzJobBean {

  private JdbcTemplate jdbcTemplate;

  private AlertsResultSetExtractor resultSetExtractor;

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

    System.out.println("Job name:" + context.getJobDetail().getName());
    JobDataMap jobMap = context.getJobDetail().getJobDataMap();

    String sqlQuery = jobMap.get("sql-query").toString();
    String mailSubject = jobMap.get("mail-subject").toString();

    List<String> params = (List<String>) jobMap.get("params");

    Pattern p = Pattern.compile("\\?");
    Matcher m = p.matcher(sqlQuery);
    int count = 0;
    while (m.find()) {
      count += 1;
    }

    Object[] parameters = new Object[count];

    for (int i = 0; i < count; i++) {
      parameters[i] = params.get(i);
    }

    System.out.println(sqlQuery);

    Object result = jdbcTemplate.query(sqlQuery, parameters, resultSetExtractor);

    List<String> resultList = (ArrayList<String>) result;
    System.out.println(resultList);
    
  }

  public JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

   public AlertsResultSetExtractor getResultSetExtractor() {
    return resultSetExtractor;
  }

  public void setResultSetExtractor(AlertsResultSetExtractor resultSetExtractor) {
    this.resultSetExtractor = resultSetExtractor;
  }

}
