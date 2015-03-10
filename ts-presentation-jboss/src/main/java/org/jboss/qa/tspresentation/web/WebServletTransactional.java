package org.jboss.qa.tspresentation.web;

import java.io.IOException;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name="WebTestServletTransactional", urlPatterns={"/transactional"})
public class WebServletTransactional extends HttpServlet {
        private static final Logger log = LoggerFactory.getLogger(WebServletTransactional.class);
        private static final long serialVersionUID = 1L;

        @Resource
        private UserTransaction utx;

        @EJB
        private StatelessBean bean;

        @Transactional(value = TxType.REQUIRES_NEW)
        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
            int status = -1;
            try {
                log.info("Starting servlet - transaction {} state {} thread {}", utx, utx.getStatus(), Thread.currentThread());
                status = utx.getStatus();
            } catch (Exception e) {
                log.info("Can't get state of transaction {}", utx);
            }

            if(status == Status.STATUS_NO_TRANSACTION) {
                throw new RuntimeException("Not transactional");
            }
        }
}
