package br.com.granatto.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.granatto.model.Cambio;
import br.com.granatto.repository.CambioRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@RestController
@RequestMapping("cambio-service")
public class CambioController {
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private CambioRepository repository;

	@GetMapping(value = "/{amount}/{from}/{to}")
	@Retry(name = "default")
	@CircuitBreaker(name = "default")
	@RateLimiter(name = "default")
	@Bulkhead(name = "default")
	public Cambio getCambio(
			@PathVariable("amount") BigDecimal amount,
			@PathVariable("from") String from,
			@PathVariable("to") String to
			) {
		
		var cambio = repository.findByFromAndTo(from , to);
		
		if(cambio == null) throw  new RuntimeException("Currency unsupported");
		
		var port = environment.getProperty("local.server.port");
		
		BigDecimal conversionFactor = cambio.getConversionFactor();
		
		BigDecimal convertedValue = conversionFactor.multiply(amount);
		
		cambio.setConvertedValued(convertedValue.setScale(2, RoundingMode.CEILING));
		
		cambio.setEnviroment(port);
		
		return cambio;
	}
}
