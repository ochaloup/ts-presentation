package org.jboss.qa.tspresentation.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

@WebServlet(name="WebTestServletResource", urlPatterns={"/start-only-resource"})
public class WebServletStartTransactionByResource extends HttpServlet{
        private static final long serialVersionUID = 1L;

        @Resource
        private UserTransaction utx;

        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
            try {
                utx.begin();
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.print("started");
            } catch (Exception e) {
                throw new RuntimeException("Can't start transaction", e);
            }
        }
}
