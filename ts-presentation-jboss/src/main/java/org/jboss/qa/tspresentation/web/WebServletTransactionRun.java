package org.jboss.qa.tspresentation.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name="WebTestServletTransactionRun", urlPatterns={"/transaction-run"})
public class WebServletTransactionRun extends HttpServlet {
        private static final Logger log = LoggerFactory.getLogger(WebServletTransactionRun.class);
        private static final long serialVersionUID = 1L;

        /**
         * User transaction object is thread safe and the transaction is used from different threads
         * so this will work fine
         */
        @Resource
        private UserTransaction utx;

        @EJB
        private StatelessBean bean;

        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

            try {
                log.info("Starting servlet - transaction {} state {}", utx, utx.getStatus());
            } catch (Exception e) {
                log.info("Can't get state of transaction {}", utx);
            }

            try {
                log.info("Starting transaction");
                utx.begin();

                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();

                bean.doSomeWork();

                log.info("Commiting transaction");
                utx.commit();

                out.print("done");
            } catch (Exception e) {
                try {
                    log.info("Rollbacking transaction - cause: {}", e);
                    utx.rollback();
                } catch (Exception erollback) {
                    log.error("Can't rollback transaction {}", erollback);
                }
            }
        }
}
