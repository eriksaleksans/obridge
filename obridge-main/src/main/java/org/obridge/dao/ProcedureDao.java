package org.obridge.dao;

import org.obridge.model.data.OraclePackage;
import org.obridge.model.data.Procedure;
import org.obridge.model.data.ProcedureArgument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * User: fkarsany Date: 2013.11.18.
 */
public class ProcedureDao {

    private JdbcTemplate jdbcTemplate;

    public ProcedureDao(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Procedure> getAllProcedures() {
        return getAllProcedures("");
    }

    public List<Procedure> getAllProcedures(String pckName) {

        String srch = "";

        if (pckName == null || pckName.isEmpty() || "".equals(pckName)) {
            srch = "%";
        } else {
            srch = pckName;
        }

        List<Procedure> procedures = jdbcTemplate.query(
                "Select object_name\n"
                        + "      ,procedure_name\n"
                        + "      ,overload\n"
                        + "      ,(Select Count(*)\n"
                        + "         From user_arguments a\n"
                        + "        Where a.object_name = t.procedure_name\n"
                        + "          And a.package_name = t.object_name\n"
                        + "          And nvl(a.overload,'##NVL##') = nvl(t.overload,'##NVL##')\n"
                        + "          And a.argument_name Is Null\n"
                        + "          And a.data_level = 0"
                        + "          And a.data_type is not null) proc_or_func\n"
                        + "  From user_procedures t\n"
                        + " Where procedure_name Is Not Null\n"
                        + "   And object_type = 'PACKAGE'"
                        + "   And object_name like ? "
                        + " and not ((object_name, procedure_name, nvl(overload, -1)) In\n"
                        + "       (Select package_name,\n"
                        + "               object_name,\n"
                        + "               nvl(overload, -1)\n"
                        + "          From user_arguments\n"
                        + "         Where data_type in ( 'REF CURSOR' , 'PL/SQL RECORD', 'PL/SQL TABLE' )) or procedure_name = 'ASSERT')\n",
                new RowMapper<Procedure>() {
                    @Override
                    public Procedure mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new Procedure(
                                resultSet.getString("object_name"),
                                resultSet.getString("procedure_name"),
                                resultSet.getString("overload") == null ? "" : resultSet.getString("overload"),
                                resultSet.getInt("proc_or_func") == 0 ? "PROCEDURE" : "FUNCTION",
                                getProcedureArguments(resultSet.getString("object_name"),
                                        resultSet.getString("procedure_name"),
                                        resultSet.getString("overload"))
                        );
                    }
                }, srch
        );

        return procedures;
    }

    public List<ProcedureArgument> getProcedureArguments(String packageName, String procedureName, String overLoadNo) {
        List<ProcedureArgument> procedureArguments = jdbcTemplate.query(
                "  select argument_name, data_type, type_name, defaulted, in_out, rownum sequen  from (Select argument_name, data_type, type_name, defaulted, in_out\n"
                        + "        From user_arguments t\n"
                        + "       Where nvl(t.package_name, '###') = nvl(upper(?), '###')\n"
                        + "         And t.object_name = upper(?)\n"
                        + "         And nvl(t.overload, '###') = nvl(?, '###')\n"
                        + "         And t.data_level = 0\n"
                        + "         And not(pls_type is null and argument_name is null and data_type is null)"
                        + "       Order By t.sequence)\n",
                new Object[]{packageName, procedureName, overLoadNo}, new RowMapper<ProcedureArgument>() {
                    @Override
                    public ProcedureArgument mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new ProcedureArgument(
                                resultSet.getString("argument_name"),
                                resultSet.getString("data_type"),
                                resultSet.getString("type_name"),
                                resultSet.getString("defaulted"),
                                resultSet.getString("in_out").contains("IN") ? true : false,
                                resultSet.getString("in_out").contains("OUT") ? true : false,
                                resultSet.getInt("sequen")
                        );
                    }
                }
        );
        return procedureArguments;
    }

    public List<OraclePackage> getAllPackages() {
        List<OraclePackage> p = jdbcTemplate.query("select object_name from user_objects where object_type = 'PACKAGE'", new RowMapper<OraclePackage>() {
            @Override
            public OraclePackage mapRow(ResultSet resultSet, int i) throws SQLException {
                OraclePackage p = new OraclePackage();
                p.setName(resultSet.getString("object_name"));
                p.setProcedureList(getAllProcedures(resultSet.getString("object_name")));
                return p;
            }
        });

        return p;
    }

}
