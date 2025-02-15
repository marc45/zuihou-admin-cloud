package com.github.zuihou.authority.service.defaults.impl;

import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import com.github.zuihou.authority.dao.defaults.InitDbMapper;
import com.github.zuihou.authority.service.defaults.InitSystemService;
import com.github.zuihou.exception.BizException;

import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 初始化系统
 *
 * @author tangyh
 * @date 2019/10/25
 */
@Service
public class InitSystemServiceImpl implements InitSystemService {
    /**
     * 需要初始化的sql文件在classpath中的路径
     */
    private final static String SQL_RESOURCE_PATH = "sqls/%s.sql";
    /**
     * 需要初始化的库
     */
    private final static List<String> INIT_DATABASE_LIST = Arrays.asList("zuihou_base");
    @Resource(name = "authorityDataSource")
    DataSource dataSource;
    @Autowired
    private InitDbMapper initDbMapper;

    @Override
    public void init(String tenant) {
        this.initDatabases(tenant);
        this.initTables(tenant);
        this.initData(tenant);
    }

    @Override
    public void initDatabases(String tenant) {
        INIT_DATABASE_LIST.forEach((database) -> initDbMapper.createDatabase(StrUtil.join(StrUtil.UNDERLINE, database, tenant)));
    }

    @Override
    public void initTables(String tenant) {
        ScriptRunner runner = null;
        try {
            runner = getScriptRunner();
            for (String database : INIT_DATABASE_LIST) {
                useDb(tenant, runner, database);
                runner.runScript(Resources.getResourceAsReader(String.format(SQL_RESOURCE_PATH, database)));
            }
        } catch (Exception e) {
            new BizException(-1, "初始化表失败");
        } finally {
            try {
                if (runner != null) {
                    runner.closeConnection();
                }
            } catch (Exception e) {
                new BizException(-1, "提交失败");
            }
        }
    }

    /**
     * 角色表
     * 菜单表
     * 资源表
     *
     * @param tenant
     */
    @Override
    public void initData(String tenant) {
        ScriptRunner runner = null;
        try {
            runner = getScriptRunner();

            for (String database : INIT_DATABASE_LIST) {
                useDb(tenant, runner, database);
                String dataScript = database + "_data";
                runner.runScript(Resources.getResourceAsReader(String.format(SQL_RESOURCE_PATH, dataScript)));
            }
        } catch (Exception e) {
            new BizException(-1, "初始化表失败");
        } finally {
            try {
                if (runner != null) {
                    runner.closeConnection();
                }
            } catch (Exception e) {
                new BizException(-1, "提交失败");
            }
        }
    }

    private String useDb(String tenant, ScriptRunner runner, String database) {
        String db = StrUtil.join(StrUtil.UNDERLINE, database, tenant);
        Reader reader = new StringReader("use " + db + ";");
        runner.runScript(reader);
        return db;
    }

    @SuppressWarnings("AlibabaRemoveCommentedCode")
    public ScriptRunner getScriptRunner() {
        try {
            Connection connection = dataSource.getConnection();
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setAutoCommit(false);
            //遇见错误是否停止
            runner.setStopOnError(true);
            /*
             * 按照那种方式执行 方式一：true则获取整个脚本并执行； 方式二：false则按照自定义的分隔符每行执行；
             */
            runner.setSendFullScript(true);
            // 设置是否输出日志，null不输出日志，不设置自动将日志输出到控制台
            // runner.setLogWriter(null);

            Resources.setCharset(Charset.forName("UTF8"));

            //设置分隔符 runner.setDelimiter(";");
            runner.setFullLineDelimiter(false);
            return runner;
        } catch (Exception ex) {
            new BizException(-1, "获取连接失败");
        }
        return null;
    }
}
