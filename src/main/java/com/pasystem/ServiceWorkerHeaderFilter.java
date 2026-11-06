import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class ServiceWorkerHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Apply the header to any JavaScript file request
        if (httpRequest.getRequestURI().endsWith(".js")) {
            httpResponse.setHeader("Service-Worker-Allowed", "/");
        }

        chain.doFilter(request, response);
    }
}
