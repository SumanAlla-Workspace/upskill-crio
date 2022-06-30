
package com.crio.warmup.stock;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  // Task:
  // - Read the json file provided in the argument[0], The file is available in the classpath.
  // - Go through all of the trades in the given file,
  // - Prepare the list of all symbols a portfolio has.
  // - if "trades.json" has trades like
  // [{ "symbol": "MSFT"}, { "symbol": "AAPL"}, { "symbol": "GOOGL"}]
  // Then you should return ["MSFT", "AAPL", "GOOGL"]
  // Hints:
  // 1. Go through two functions provided - #resolveFileFromResources() and #getObjectMapper
  // Check if they are of any help to you.
  // 2. Return the list of all symbols in the same order as provided in json.

  // Note:
  // 1. There can be few unused imports, you will need to fix them to make the build pass.
  // 2. You can use "./gradlew build" to check if your code builds successfully.

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    ObjectMapper objMapper = getObjectMapper();
    File input = resolveFileFromResources(args[0]);
    PortfolioTrade[] trades = objMapper.readValue(input, PortfolioTrade[].class);
    List<String> list =
        Arrays.stream(trades).map(trade -> trade.getSymbol()).collect(Collectors.toList());
    return list;
  }


  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  // for the stocks provided in the Json.
  // Use the function you just wrote #calculateAnnualizedReturns.
  // Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  public static String getToken() {
    return "493058acf8462d880a9030f5b189c023b4f516dc";
  }

  public static double getclose(PortfolioTrade trade, String endDateString)
      throws JsonMappingException, JsonProcessingException {
    RestTemplate restTemplate = new RestTemplate();
    LocalDate endDate = LocalDate.parse(endDateString);
    String url = prepareUrl(trade, endDate, "671b3c89b9edaf3aec0390fe4ecf1b2aeb62afb5");
    String result = restTemplate.getForObject(url, String.class);
    ObjectMapper mapper = getObjectMapper();
    TiingoCandle[] tiingoCandles = mapper.readValue(result, TiingoCandle[].class);

    double sum = Stream.of(tiingoCandles).filter(
        candle -> candle.getDate().equals(endDate) || candle.getDate().equals(endDate.minusDays(1)))
        .findFirst().get().getClose();

    return sum;
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    ObjectMapper objMapper = getObjectMapper();
    File input = resolveFileFromResources(args[0]);
    PortfolioTrade[] trades = objMapper.readValue(input, PortfolioTrade[].class);
    HashMap<String, Double> resultData = new HashMap<String, Double>();
    for (int i = 0; i < trades.length; i++) {
      double sum = getclose(trades[i], args[1]);
      resultData.put(trades[i].getSymbol(), sum);
    }
    // Create a list from elements of HashMap
    List<Map.Entry<String, Double>> list =
        new LinkedList<Map.Entry<String, Double>>(resultData.entrySet());

    // Sort the list using lambda expression
    Collections.sort(list, (i1, i2) -> i1.getValue().compareTo(i2.getValue()));

    // put data from sorted list to hashmap
    HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
    for (Map.Entry<String, Double> aa : list) {
      temp.put(aa.getKey(), aa.getValue());
    }
    List<String> result = new ArrayList<String>();
    for (Map.Entry<String, Double> en : temp.entrySet()) {
      result.add(en.getKey());
    }
    return result;
  }

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  // and deserialize the results in List<Candle>

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI())
        .toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  // Follow the instructions provided in the task documentation and fill up the correct values for
  // the variables provided. First value is provided for your reference.
  // A. Put a breakpoint on the first line inside mainReadFile() which says
  // return Collections.emptyList();
  // B. Then Debug the test #mainReadFile provided in PortfoliomanagerApplicationTest.java
  // following the instructions to run the test.
  // Once you are able to run the test, perform following tasks and record the output as a
  // String in the function below.
  // Use this link to see how to evaluate expressions -
  // https://code.visualstudio.com/docs/editor/debugging#_data-inspection
  // 1. evaluate the value of "args[0]" and set the value
  // to the variable named valueOfArgument0 (This is implemented for your reference.)
  // 2. In the same window, evaluate the value of expression below and set it
  // to resultOfResolveFilePathArgs0
  // expression ==> resolveFileFromResources(args[0])
  // 3. In the same window, evaluate the value of expression below and set it
  // to toStringOfObjectMapper.
  // You might see some garbage numbers in the output. Dont worry, its expected.
  // expression ==> getObjectMapper().toString()
  // 4. Now Go to the debug window and open stack trace. Put the name of the function you see at
  // second place from top to variable functionNameFromTestFileInStackTrace
  // 5. In the same window, you will see the line number of the function in the stack trace window.
  // assign the same to lineNumberFromTestFileInStackTrace
  // Once you are done with above, just run the corresponding test and
  // make sure its working as expected. use below command to do the same.
  // ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 =
        "/home/crio-user/workspace/alla-suman09-ME_QMONEY_V2/qmoney/src/main/resources/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@6d9f7a80";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplicationTest.mainReadFile()";
    String lineNumberFromTestFileInStackTrace = "";



    return Arrays.asList(
        new String[] {valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
            functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace});
  }


  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  // TODO:
  // After refactor, make sure that the tests pass by using these two commands
  // ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  // ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename)
      throws IOException, URISyntaxException {
    ObjectMapper objMapper = getObjectMapper();
    File input = resolveFileFromResources(filename);
    PortfolioTrade[] trades = objMapper.readValue(input, PortfolioTrade[].class);
    List<PortfolioTrade> list = Arrays.stream(trades).collect(Collectors.toList());
    return list;
  }

  // TODO:
  // Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    String url = "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?"
        + "startDate=" + trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;
    // System.out.println(url);
    return url;
  }


  // TODO:
  // Ensure all tests are passing using below command
  // ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    Double openingPrice = candles.get(0).getOpen();
    return openingPrice;
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    Double closingPrice = candles.get(candles.size() - 1).getClose();
    return closingPrice;
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token)
      throws JsonMappingException, JsonProcessingException {

    RestTemplate restTemplate = new RestTemplate();
    String url = prepareUrl(trade, endDate, "671b3c89b9edaf3aec0390fe4ecf1b2aeb62afb5");
    String result = restTemplate.getForObject(url, String.class);
    ObjectMapper mapper = getObjectMapper();
    TiingoCandle[] tiingoCandles = mapper.readValue(result, TiingoCandle[].class);
    return Arrays.asList(tiingoCandles);
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException, JsonParseException {
    File file = resolveFileFromResources(args[0]);
    LocalDate endDate = LocalDate.parse(args[1]);
    byte[] byteArray = Files.readAllBytes(file.toPath());
    String content = new String(byteArray, "UTF8");

    ObjectMapper mapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = mapper.readValue(content, PortfolioTrade[].class);

    String token = "671b3c89b9edaf3aec0390fe4ecf1b2aeb62afb5";// getToken();
    String uri =
        "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

    List<AnnualizedReturn> annualReturn = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {

      String url = uri.replace("$APIKEY", token).replace("$SYMBOL", portfolioTrade.getSymbol())
          .replace("$STARTDATE", portfolioTrade.getPurchaseDate().toString())
          .replace("$ENDDATE", endDate.toString());


      RestTemplate restTemplate = new RestTemplate();
      String result = "";
      try {
        result = restTemplate.getForObject(url, String.class);
      } catch (Exception e) {
        throw new RuntimeException();
      }
      TiingoCandle[] tiingoCandles = mapper.readValue(result, TiingoCandle[].class);
      // System.out.println(result + " suman");
      double buyprice = Stream.of(tiingoCandles)
          .filter(candle -> candle.getDate().equals(portfolioTrade.getPurchaseDate())).findFirst()
          .get().getOpen();

      double sellprice;
      Optional<TiingoCandle> optional =
          Stream.of(tiingoCandles).filter(candle -> candle.getDate().equals(endDate)).findAny();
      boolean beforeEndDate = false;
      if (optional.isEmpty()) {
        optional = Stream.of(tiingoCandles)
            .filter(candle -> candle.getDate().equals(endDate.minusDays(1))).findFirst();
        beforeEndDate = true;
      }
      sellprice = optional.get().getClose();
      AnnualizedReturn turn = null;
      if (beforeEndDate) {
        turn =
            calculateAnnualizedReturns(endDate.minusDays(1), portfolioTrade, buyprice, sellprice);
      } else {
        turn = calculateAnnualizedReturns(endDate, portfolioTrade, buyprice, sellprice);
      }

      annualReturn.add(turn);
    }

    annualReturn.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn));
    Collections.reverse(annualReturn);

    return annualReturn;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Return the populated list of AnnualizedReturn for all stocks.
  // Annualized returns should be calculated in two steps:
  // 1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  // 1.1 Store the same as totalReturns
  // 2. Calculate extrapolated annualized returns by scaling the same in years span.
  // The formula is:
  // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  // 2.1 Store the same as annualized_returns
  // Test the same using below specified command. The build should be successful.
  // ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double totalReturn = (sellPrice - buyPrice) / buyPrice;
    double totalnumdays = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    double totalnumyears = totalnumdays / 365;
    double inv = 1 / totalnumyears;

    double annualizedreturns = Math.pow((1 + totalReturn), inv) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedreturns, totalReturn);
  }


  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Once you are done with the implementation inside PortfolioManagerImpl and
  // PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  // Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  // call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    // String contents = readFileAsString(file);
    PortfolioTrade[] portfolioTrades = readFileAsString(file);
    PortfolioManager portfolioManager =
        PortfolioManagerFactory.getPortfolioManager(new RestTemplate());
    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  public static PortfolioTrade[] readFileAsString(String fileName)
      throws URISyntaxException, IOException {
    File file = resolveFileFromResources(fileName);
    byte[] byteArray = Files.readAllBytes(file.toPath());
    String content = new String(byteArray, "UTF8");
    ObjectMapper mapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = mapper.readValue(content, PortfolioTrade[].class);
    return portfolioTrades;
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());



  }
}

