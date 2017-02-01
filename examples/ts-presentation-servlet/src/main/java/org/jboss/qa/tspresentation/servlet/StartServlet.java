package org.jboss.qa.tspresentation.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name="StartServlet", urlPatterns={"/"})
public class StartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    private MyBean bean;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    		throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<p>Calling bean: <b>" + bean.getClass().getName() + "</b></p>");
        bean.call();
    }
}
