package com.bestbuy.eos.alerts;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bestbuy.eos.jaxb.JobDetails;
import com.bestbuy.eos.jaxb.Response;
import com.bestbuy.eos.jaxb.RunningJobs;

@Controller
public class JobsController implements ApplicationContextAware {

  private ApplicationContext context;

  @Inject
  private Scheduler scheduler;

  @RequestMapping(value = "/addJob", method = RequestMethod.POST)
  public @ResponseBody
  Response addJobtoScheduler(@RequestBody JobDetails job, HttpServletRequest httpRequest, HttpServletResponse httpServletResponse) throws HttpMessageNotWritableException, IOException {

    HttpStatus status = HttpStatus.OK;
    Response response = new Response();

    try {

      String[] currentJobs = scheduler.getJobNames("EOS");
      List<String> runningJobs = Arrays.asList(currentJobs);

      if (runningJobs.contains(job.getName().trim())) {
        httpServletResponse.setStatus(400);
        response.setMessage("Job named:" + job.getName() + " already running so failed to add new job");
        return response;
      }

      JobDetail jobDetail = (JobDetail) context.getBean("jobDetailBean");
      jobDetail.setName(job.getName());
      jobDetail.setGroup("EOS");

      Map<String, Object> jobMap = new HashMap<String, Object>();
      jobMap.put("sql-query", job.getSqlQuery());
      jobMap.put("params", job.getParams().getParam());
      jobMap.put("mail-subject", job.getMailDetails().getSubject());

      jobDetail.getJobDataMap().putAll(jobMap);

      CronTrigger trigger = new CronTrigger();
      trigger.setName(job.getName() + "_trigger");

      trigger.setCronExpression(job.getCronExpression());

      trigger.setJobName(jobDetail.getName());
      trigger.setJobGroup(jobDetail.getGroup());

      scheduler.scheduleJob(jobDetail, trigger);

      response.setMessage("Successfully added Job :" + job.getName() + " with cron:" + job.getCronExpression());

    } catch (ParseException e) {
      status = HttpStatus.BAD_REQUEST;
      response.setMessage("Unable to schedule job as Cron expression provided is invalid");
    } catch (SchedulerException e) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
      response.setMessage("Unable to schedule job for the provided job data");
    }

    httpServletResponse.setStatus(status.value());

    return response;
  }

  @RequestMapping(value = "/getJobs", method = RequestMethod.GET)
  public @ResponseBody
  RunningJobs getAllRunningJobs(HttpServletRequest httpRequest, HttpServletResponse httpServletResponse) throws HttpMessageNotWritableException, IOException {

    HttpStatus status = HttpStatus.OK;

    RunningJobs jobs = new RunningJobs();
    List<RunningJobs.Job> listOfJobs = jobs.getJob();

    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    try {
      for (String jobName : scheduler.getJobNames("EOS")) {

        RunningJobs.Job job = new RunningJobs.Job();
        job.setName(jobName);

        Trigger[] triggers = scheduler.getTriggersOfJob(jobName, "EOS");

        Date nextFireTime = triggers[0].getNextFireTime();
        job.setNextFireTime(formatter.format(nextFireTime));

        Date previousFireTime = triggers[0].getPreviousFireTime();
        job.setLastFireTime(formatter.format(previousFireTime));

        JobDetail jobDetail = scheduler.getJobDetail(jobName, "EOS");

        JobDataMap jobMap = jobDetail.getJobDataMap();
        String sql = jobMap.get("sql-query").toString();

        job.setSqlQuery(sql);

        List<String> params = (List<String>) jobMap.get("params");
        RunningJobs.Job.Params sqlParams = new RunningJobs.Job.Params();
        sqlParams.setParam(params);
        job.setParams(sqlParams);

        listOfJobs.add(job);
      }
      status = HttpStatus.OK;

    } catch (SchedulerException e) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    httpServletResponse.setStatus(status.value());
    return jobs;

  }

  @RequestMapping(value = "/deleteJob/{jobName}", method = RequestMethod.DELETE)
  public @ResponseBody
  Response deleteJobFromScheduler(@PathVariable String jobName, HttpServletRequest httpRequest, HttpServletResponse httpServletResponse) throws HttpMessageNotWritableException, IOException {

    HttpStatus status = HttpStatus.OK;
    Response response = new Response();

    String[] currentJobs = null;
    try {
      currentJobs = scheduler.getJobNames("EOS");

      List<String> runningJobs = Arrays.asList(currentJobs);

      if (!runningJobs.contains(jobName.trim())) {
        status = HttpStatus.BAD_REQUEST;
        response.setMessage("Job named:" + jobName + " doesn't exist to delete");

      } else {
        scheduler.deleteJob(jobName, "EOS");
        status = HttpStatus.OK;
        response.setMessage("Succcessfully deleted job :" + jobName);
      }

    } catch (SchedulerException e) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
      response.setMessage("Error happened while deleting job");
    }
    httpServletResponse.setStatus(status.value());

    return response;

  }

  public void setApplicationContext(ApplicationContext context) throws BeansException {
    this.context = context;
  }

}
