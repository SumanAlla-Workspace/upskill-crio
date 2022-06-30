
package com.crio.warmup.stock.quotes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
    ObjectMapper mapper = getObjectMapper();
    // RestTemplate restTemplate = new RestTemplate();
    // PortfolioManagerImpl(restTemplate);
    if (to.isBefore(from)) {
      to = LocalDate.now();
    }
    String url = buildUri(symbol, from, to);
    String result = null;
    try {
      result = this.restTemplate.getForObject(url, String.class);
    } catch (Exception e) {
      throw new StockQuoteServiceException("Unable to fetch the response");
    }
    if (result == null)
      throw new StockQuoteServiceException("Unable to fetch the response");
    return Arrays.asList(mapper.readValue(result, TiingoCandle[].class));
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  // Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  // ./gradlew test --tests TiingoServiceTest


  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  // Write a method to create appropriate url to call the Tiingo API.
  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token = "493058acf8462d880a9030f5b189c023b4f516dc";
    String uriTemplate = String.format(
        "https://api.tiingo.com/tiingo/daily/%s/prices?" + "startDate=%s&endDate=%s&token=%s",
        symbol, startDate.toString(), endDate.toString(), token);
    return uriTemplate;
  }
}
