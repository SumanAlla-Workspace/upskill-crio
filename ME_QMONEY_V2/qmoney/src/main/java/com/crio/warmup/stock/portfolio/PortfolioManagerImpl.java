
package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService, RestTemplate restTemplate) {
    this.stockQuotesService = stockQuotesService;
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  // Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  /*
   * private Comparator<AnnualizedReturn> getComparator() { return
   * Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed(); }
   */

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException {
    List<AnnualizedReturn> annualReturn = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {

      List<Candle> tiingoCandles = null;
      try {
        tiingoCandles = this.stockQuotesService.getStockQuote(portfolioTrade.getSymbol(),
            portfolioTrade.getPurchaseDate(), endDate);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }

      int no = tiingoCandles.size();
      int flag = 1;

      if (portfolioTrade.getPurchaseDate().isAfter(endDate)) {
        flag = 0;

      }
      LocalDate nodata;
      if (((tiingoCandles.get(no - 1)).getDate()).equals(endDate)) {
        nodata = endDate;
      }

      else if (flag == 0) {
        nodata = LocalDate.now();
      }

      else {
        nodata = tiingoCandles.get(no - 1).getDate();
      }
      double sellprice = tiingoCandles.stream()
          .filter(candle -> candle.getDate().equals(endDate) || candle.getDate().equals(nodata))
          .findFirst().get().getClose();

      // System.out.println(sellprice);
      System.out.println(portfolioTrade.getSymbol() + " :: " + portfolioTrade.getPurchaseDate());
      double buyprice = tiingoCandles.stream()
          .filter(candle -> candle.getDate().equals(portfolioTrade.getPurchaseDate())).findFirst()
          .get().getOpen();
      // System.out.println(buyprice);


      // System.out.println(buyPrice);
      // System.out.println(sellPrice);
      double totalReturn = (sellprice - buyprice) / buyprice;
      double totalnumdays = ChronoUnit.DAYS.between(portfolioTrade.getPurchaseDate(), endDate);
      // System.out.println(totalnumdays);
      double totalnumyears = totalnumdays / 365;
      double inv = 1 / totalnumyears;
      // System.out.println(totalnumyears);
      // System.out.println(totalReturn);
      double annualizedreturns = Math.pow((1 + totalReturn), inv) - 1;
      // System.out.println(annualizedreturns);

      AnnualizedReturn turn =
          new AnnualizedReturn(portfolioTrade.getSymbol(), annualizedreturns, totalReturn);

      annualReturn.add(turn);
    }
    annualReturn.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn));
    Collections.reverse(annualReturn);


    return annualReturn;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double totalReturn = (sellPrice - buyPrice) / buyPrice;
    double totalnumdays = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    double totalnumyears = totalnumdays / 365;
    double inv = 1 / totalnumyears;

    double annualizedreturns = Math.pow((1 + totalReturn), inv) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedreturns, totalReturn);
  }


  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  // Modify the function #getStockQuote and start delegating to calls to
  // stockQuoteService provided via newly added constructor of the class.
  // You also have a liberty to completely get rid of that function itself, however, make sure
  // that you do not delete the #getStockQuote function.


  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    ObjectMapper mapper = getObjectMapper();
    // RestTemplate restTemplate = new RestTemplate();
    // PortfolioManagerImpl(restTemplate);
    if (to.isBefore(from)) {
      to = LocalDate.now();
    }
    String url = buildUri(symbol, from, to);
    String result = this.restTemplate.getForObject(url, String.class);

    return Arrays.asList(mapper.readValue(result, TiingoCandle[].class));
  }

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



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws InterruptedException, StockQuoteServiceException {
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Future<AnnualizedReturn>> list = new ArrayList<Future<AnnualizedReturn>>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      StockQuotesServiceCallable callable = new StockQuotesServiceCallable();
      callable.setStockQuotesService(stockQuotesService);
      callable.setSymbol(portfolioTrade.getSymbol());
      callable.setPurchaseDate(portfolioTrade.getPurchaseDate());
      callable.setEndDate(endDate);
      Future<AnnualizedReturn> annualizedReturn = executor.submit(callable);
      list.add(annualizedReturn);
    }
    List<AnnualizedReturn> annualReturn = new ArrayList<>();
    for (Future<AnnualizedReturn> fut : list) {
      try {
        annualReturn.add(fut.get());
      } catch (InterruptedException | ExecutionException e) {
        //e.printStackTrace();
        throw new StockQuoteServiceException("");
      }
    }

    annualReturn.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn));
    Collections.reverse(annualReturn);
    return annualReturn;
  }



}
