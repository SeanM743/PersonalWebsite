Four tasks I want you to work on now:

1. When I startup the service, I see API calls to yahoo server. There should not be any requests to the server for historical data or real time data because it is Sunday and the market is closed. We should not be adding entries for the days when the market is closed. Can you look at the business logic for this part and update it?

2. The subheader which says last updated is reporting a date of 1/26. we should put current date or at least say something like updated as of xxx where xxx is the latest market time when it was open

3. I want to make a refactor of the stock portfolio tab. Right now we have a chart of current holdings. I want to reduce the widget of that chart by 50%. In that new area, let's have another container which is close but not touching the holdings table, and show a graph of the stock performance over time. we can use the actual performance and have time slots like 1D, 1W, 1M, 6M, 1YR which gets retrieved by the yahoo api. add a spinner to this component and cache it so we don't excessively ping the api

 the free space above the new container, let's add some key market prices, like the DOW, Nasdaq, S&P 500, Bitcon price, gold price and what the percent change for the day is with color coding. Make whatever tables you need to store this if necessary or just fetch it and store the current price in database and use similar logic as stocks to decide if we need to pull it from the api

4. The performance tab inside the monitoring page is not showing any information related to performance. Please conduct a thorough deep dive into the performance metrics / observability of the service and fix