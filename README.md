# event-counter

Application reads kafka topic events and count them using **Linear probabilistic counter**.  

1) `cd deploy`<br>
2) `docker-compose up`<br>
3) Add telegram bot `@dasdhafhakfabot`<br>
4) Send `/post string:number` command for posting new event (e.g. `/post friday:13`)<br>
5) Send `/stat` command for collecting counters from all available nodes and theirs replicas
