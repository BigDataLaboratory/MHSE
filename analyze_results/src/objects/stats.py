import numpy as np

class resultsStats:

    def __init__(self, collisionTable,lastHops ,numNodes,seedList,time,seed = 256,threshold = 0.9):
        self.threshold = threshold
        self.seed = seed
        self.time = sum(time[0:self.seed])/len(time[0:self.seed])

        self.maxHop = max(lastHops[0:self.seed])
        if(type(collisionTable) == list):
            self.collsionTable = self.tansform_to_dic(collisionTable)
        else:
            self.collsionTable = self.lowerBoundDiameter(collisionTable)
        self.completeSeedList = seedList
        self.numNodes = numNodes

        self.totalCouples = self.totalCouples()
        self.couplesPercentage = self.totalCouplesPercentage()

        self.avgDistance = self.avgDistance()
        self.lowerBoundDiameter = len(self.collsionTable.keys())-1

        self.effectiveDiameter = self.effectiveDiameter()

    def tansform_to_dic(self,m):



        new_matrix = [[m[j][i] for j in range(len(m))] for i in range(len(m[0]))]

        dic = {}


        for i in range(0,len(new_matrix)):
            dic[str(i)] = new_matrix[i][:]
        return(dic)

    def lowerBoundDiameter(self,collisionTable):
        newCollisionTable = {}

        for hop in range(0,self.maxHop+1):
            newCollisionTable[str(hop)] = collisionTable[str(hop)]

        return(newCollisionTable)

    def totalCouples(self):

        overallCollsions = self.collsionTable[str(self.maxHop)][0:self.seed]
        numTotalCollisions = sum(overallCollsions)
        totalCouplesReachable = numTotalCollisions * self.numNodes / self.seed

        return (totalCouplesReachable)


    def totalCouplesPercentage(self):

        couplesPercentage = self.totalCouples * self.threshold

        return (couplesPercentage)

    def avgDistance(self):
        sumAvg = 0

        for hop in range(0,self.maxHop+1):

            collisions = self.collsionTable[str(hop)][0:self.seed]
            totalCollisions = sum(collisions)
            if(hop != 0):
                previousHop = hop - 1
                previousCollisions = self.collsionTable[str(previousHop)][0:self.seed]
                previousTotalCollisions = sum(previousCollisions)
                couplesReachable = totalCollisions * self.numNodes / self.seed
                previousCouplesReachable = previousTotalCollisions * self.numNodes / self.seed
                couplesReachableForHop = couplesReachable - previousCouplesReachable
                sumAvg += hop*couplesReachableForHop

        return(sumAvg/self.totalCouples)

    def interpolate(self,y0,y1,y):
        if(y1-y0 == 0):
            return(0)
        return (y - y0) / (y1 - y0)



    def effectiveDiameter(self):
        if(len(self.collsionTable.values())==0):
            return(0)
        d = 1

        numCollisions = sum(self.collsionTable[str(self.lowerBoundDiameter)][0:self.seed])

        while(sum(self.collsionTable[str(d)][0:self.seed])/numCollisions < self.threshold):
            d += 1

        collisionsD = sum(self.collsionTable[str(d)][0:self.seed])

        previousCollisionsD = sum(self.collsionTable[str(d - 1)][0:self.seed])

        couplesD = collisionsD * self.numNodes / self.seed

        previousCouplesD = previousCollisionsD * self.numNodes / self.seed


        interpolation = self.interpolate(previousCouplesD,couplesD,self.couplesPercentage)
        result = (d - 1) + interpolation


        if (result < 0):
            result = 0

        return result

    def printStats(self):
        print("----------- STATS -----------")
        print("Seed number ", self.seed)
        print("Avg distance %.20f" %self.avgDistance)
        print("Total couples %.20f" %self.totalCouples)
        print("Total couples percentage %.20f" %self.couplesPercentage)
        print("Lowerbound diameter ", self.lowerBoundDiameter)
        print("Effective diameter %.20f" %self.effectiveDiameter)
        print("-----------------------------")

    def get_stats(self,additionalInfo = None):

        if(additionalInfo!= None):
            additionalInfo['avg_distance'] = self.avgDistance
            additionalInfo['total_couples'] = self.totalCouples
            additionalInfo['total_couples_perc'] = self.couplesPercentage
            additionalInfo['lower_bound'] = self.lowerBoundDiameter
            additionalInfo['effective_diameter'] = self.effectiveDiameter
            additionalInfo['treshold'] = self.threshold
            additionalInfo['num_seed'] = self.seed
            additionalInfo['time'] = self.time
            #additionalInfo['seedsList'] = self.transform_seedlist(self.completeSeedList)
        else:
            additionalInfo ={
                'avg_distance':self.avgDistance,
                'total_couples': self.totalCouples,
                'total_couples_perc':self.couplesPercentage,
                'lower_bound': self.lowerBoundDiameter,
                'effective_diameter':self.effectiveDiameter,
                'treshold':self.threshold,
                'num_seed':self.seed,
                'time': self.time

                #'seedsList':self.transform_seedlist(self.completeSeedList)

            }

        return(additionalInfo)

    def transform_seedlist(self,seedListString):
        seedList = []

        for seed in seedListString.split(","):
            if(seed != ""):
                seedList.append(seed)

        newSeedList = ""

        for seed in seedList[0:self.seed]:
            newSeedList += seed
            newSeedList += ","
        return(newSeedList)