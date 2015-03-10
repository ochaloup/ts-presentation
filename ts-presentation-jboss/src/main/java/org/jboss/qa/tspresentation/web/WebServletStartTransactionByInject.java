package org.jboss.qa.tspresentation.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name="WebTestServletInject", urlPatterns={"/start-only-inject"})
public class WebServletStartTransactionByInject extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(WebServletStartTransactionByInject.class);
    private static final long serialVersionUID = 1L;

    @Inject
    private UserTransaction utx;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        try {
            log.info("Utx {} has status {} at thread {}", utx, utx.getStatus(), Thread.currentThread());
        } catch (Exception e) {
            // ignore
        }

        try {
            utx.begin();
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print("started-inject");
        } catch (Exception e) {
            throw new RuntimeException("Can't start transaction", e);
        }
    }
}
