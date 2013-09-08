package com.bestbuy.eos.alerts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component("resultSetExtractor")
@Scope("prototype")
public class AlertsResultSetExtractor implements ResultSetExtractor<Object> {

  public Object extractData(ResultSet rs) throws SQLException, DataAccessException {

    List<String> results = new ArrayList<String>();

    while (rs.next()) {

      results.add(rs.getString(1));
      results.add(rs.getString(2));
      results.add(rs.getString(3));

    }

    return results;

  }
}
