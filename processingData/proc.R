library(tidyverse)
library(ggplot2)
library(tidyr)

setwd("~/Documents/GitHub/info305_mile2/processingData/")

sr16ws16 <- read_csv("acc_1566963020078SR60WS32.0.csv")#originals


#################
# final results #
#################
#standing finals
standing <- read_csv("acc_standing.csv")
standing <- standing[-(1:2)]
standing.mean <- as_data_frame(colMeans(standing[-1]))
standing.mean.tall <- standing.mean %>% gather()
standing.mean.total <- colMeans(standing.mean.tall)


standing.abs <- apply(standing[-(1)], 2, abs)
standing.range <- as_tibble(apply(standing.abs, 2, range)) %>% gather()

#walking finals
walking <- read_csv("acc_walking.csv")
walking <- walking[-(1:2)]
walking.mean <- as_tibble(colMeans(walking[-1]))
walking.mean.tall <- walking.mean %>% gather()
walking.mean.total <- colMeans(walking.mean.tall)

walking.abs <- apply(walking[-(1)], 2, abs)
walking.range <- as_tibble(apply(walking.abs, 2, range)) %>% gather()

#jogging finals
jogging <- read_csv("acc_jogging.csv")
jogging <- jogging[-(1:2)]
jogging.mean <- as_tibble(colMeans(jogging[-1]))
jogging.mean.tall <- jogging.mean %>% gather()
jogging.mean.total <- colMeans(jogging.mean.tall)

jogging.abs <- apply(jogging[-(1)], 2, abs)
jogging.range <- as_tibble(apply(jogging.abs, 2, range)) %>% gather()

####################
# Graphing results #
####################
#mean plot for all
ggplot() +
    geom_line(data = standing.mean.tall, aes(x=seq(1:63), y=abs(value)), color = 'red') +
    geom_line(data = walking.mean.tall, aes(x=seq(1:63), y=abs(value)), color = 'blue') +
    geom_line(data = jogging.mean.tall, aes(x=seq(1:63), y=abs(value)), color = 'green') +
  scale_x_continuous("frequency bin", seq(1:63))

ggplot() +
  geom_point(data = standing.range, aes(x=seq(1:126), y=abs(value)), color = 'red') +
  geom_point(data = walking.range, aes(x=seq(1:126), y=abs(value)), color = 'blue') +
  geom_point(data = jogging.range, aes(x=seq(1:126), y=abs(value)), color = 'green') +
  scale_x_continuous("frequency bin", seq(1:63))
  
#########################
# 
collection <- mutate(standing.mean.tall)


####################

mTest <- as_data_frame(colMeans(sr16ws16))

# remove 
rangeTest <- range(sr16ws16[[10]])

#remove samplenumber & Magnitude
noSRorMag <-  sr16ws16[-(1:2)]

#standing calibration
standing <- noSRorMag[]
standing.mean <- as_data_frame(colMeans(standing[-1]))
standing.mean.tall <- standing.mean %>% gather()
standing.mean.total <- colMeans(standing.mean.tall)
standing.range <- range((abs(standing[32])))


#walking data
walking <- noSRorMag[]
walking.mean <- as_tibble(colMeans(walking[-1]))
walking.mean.tall <- walking.mean %>% gather()
walking.mean.total <- colMeans(walking.mean.tall)




ggplot() +
  geom_line(data =walking.mean.tall, aes(x=seq(1:31), y=abs(value)))
#+
 # geom_line(data = standing.mean.tall, aes(x=seq(1:255), y=value), colour = 'red')


#to tall
standingTall <- standing %>% gather(frequencyBand, val, seq(0:15), factor_key = TRUE)

walkingTall <- walking %>% gather(frequencyBand, val, seq(0:15), factor_key = TRUE)

ggplot(walking.mean) + 
  geom_point(aes(x=))
#box and whisker 

#using original data to plot each frequ
allTall <- noSRorMag %>% gather(frequenceBand, val, seq(0:15), factor_key = TRUE)

ggplot(allTall) +
  geom_line(abs(x=frequencyBand, color=frequencyBand))
