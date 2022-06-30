package com.crio.warmup.stock.portfolio;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;

public class StockQuotesServiceCallable implements Callable<AnnualizedReturn> {

    private StockQuotesService stockQuotesService;
    private String symbol;
    private LocalDate purchaseDate;
    private LocalDate endDate;

    public StockQuotesServiceCallable() {

    }

    @Override
    public AnnualizedReturn call() throws Exception {

        List<Candle> tiingoCandles = null;
        try {
            tiingoCandles = this.stockQuotesService.getStockQuote(this.getSymbol(),
                    this.getPurchaseDate(), this.getEndDate());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        int no = tiingoCandles.size();
        int flag = 1;

        if (this.getPurchaseDate().isAfter(endDate)) {
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
        double sellprice = tiingoCandles.stream().filter(
                candle -> candle.getDate().equals(endDate) || candle.getDate().equals(nodata))
                .findFirst().get().getClose();

        // System.out.println(sellprice);
        System.out.println(this.getSymbol() + " :: " + this.getPurchaseDate());
        double buyprice = tiingoCandles.stream()
                .filter(candle -> candle.getDate().equals(this.getPurchaseDate())).findFirst().get()
                .getOpen();
        // System.out.println(buyprice);


        // System.out.println(buyPrice);
        // System.out.println(sellPrice);
        double totalReturn = (sellprice - buyprice) / buyprice;
        double totalnumdays = ChronoUnit.DAYS.between(this.getPurchaseDate(), endDate);
        // System.out.println(totalnumdays);
        double totalnumyears = totalnumdays / 365;
        double inv = 1 / totalnumyears;
        // System.out.println(totalnumyears);
        // System.out.println(totalReturn);
        double annualizedreturns = Math.pow((1 + totalReturn), inv) - 1;
        // System.out.println(annualizedreturns);

        AnnualizedReturn turn =
                new AnnualizedReturn(this.getSymbol(), annualizedreturns, totalReturn);
        return turn;

    }

    public StockQuotesService getStockQuotesService() {
        return stockQuotesService;
    }

    public void setStockQuotesService(StockQuotesService stockQuotesService) {
        this.stockQuotesService = stockQuotesService;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }


}
