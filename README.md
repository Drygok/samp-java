<br/>
<p align="center">
  <h3 align="center">SA-MP Java utilities</h3>

  <p align="center">
    A simple package from one file for sending Ping packets to SAMP servers and receiving basic information from them - a list of players and online status on the server
    <br/>
    <br/>
  </p>
</p>

![Contributors](https://img.shields.io/github/contributors/Drygok/samp-java?color=dark-green) ![License](https://img.shields.io/github/license/Drygok/samp-java) 

## Warning!
Don't forget to close the socket after use if you don't plan to keep the connection open for a long time!
An example of use is presented below.

Based on the PHP solution: https://github.com/Westie/samp-php

### Using example: 
`SampQueryAPI sampQueryAPI = new SampQueryAPI("127.0.0.1", 7777);
int online = sampQueryAPI.getInfo().players;
sampQueryAPI.close();`