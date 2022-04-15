import json
import pandas as pd
#import numpy as np
import sys, getopt
import rpy2.robjects as ro
from src.objects.scores import summary
#from objects.stats import resultsStats




def read_json(InputPath,OutputPath,GroundTruthPath = None,std = True,Ttest = True,dataframe = True,LabelPath = None,rounding = 5):

    experiments = {}
    statistics = {}
    results = {}
    # provvisorio
    #sample_residuals = {}
    groundTruth = {}
    if((Ttest == True ) and GroundTruthPath == None ):
        print("Error! If you want to perform a Ttest between samples and groundtruth you need also the groundtruth values")
        exit(-1)
    try:
        with open(InputPath) as json_file:
            data = json.load(json_file)
    except IOError:
        print('Error! Can not open '+InputPath)

    if(GroundTruthPath != None):
        try:
            with open(GroundTruthPath) as json_gt:
                groundTruth = json.load(json_gt)
        except IOError:
            print('Error! Can not open ' + json_gt)

    seed_list = []
    algo_list = []
    in_direction = False
    out_direction = False
    hb = False
    direction_list = []
    for elem in data:
        if not(elem['num_seed'] in seed_list):
            seed_list.append(elem['num_seed'])
        if "algorithm" in elem:
            if not(elem['algorithm'] in algo_list):
                algo_list.append(elem['algorithm'])
        if 'direction' in elem:
            if(elem['direction'] == 'out'):
                out_direction = True
            elif(elem['direction'] == 'in'):
                in_direction = True

    if in_direction == False and out_direction == False:
        print("No direction in the out.file, assuming HyperBall Experiments.")
        out_direction = True
    if not algo_list:
        print("No algorithm in the out.file, assuming HyperBall Experiments.")
        algo_list = ['HyperBall']
        hb = True


    if(out_direction):
        direction_list.append("out")
    if(in_direction):
        direction_list.append("in")

    if hb:
        for i in range(0,len(data)):
            data[i]['algorithm'] = "HyperBall"
            data[i]['direction'] = "out"
            
            
    # Building a dictionary entry for each type of simulation

    for algo in algo_list:

        for direction in direction_list:
            for seed in seed_list:

                experiments[(algo,direction,seed,"avg_distance")] =[]
                experiments[(algo,direction,seed,"effective_diameter")] = []
                experiments[(algo,direction,seed,"lower_bound")] = []
                experiments[(algo,direction,seed,"total_couples")] = []
                experiments[(algo,direction,seed,"memory_used")] = []
                experiments[(algo,direction,seed,"time")] = []



    for elem in data:


        experiments[(elem['algorithm'],elem['direction'],elem['num_seed'],"avg_distance")].append(elem['avg_distance'])
        experiments[(elem['algorithm'],elem['direction'],elem['num_seed'],"effective_diameter")].append(elem['effective_diameter'])
        experiments[(elem['algorithm'],elem['direction'],elem['num_seed'],"lower_bound")].append(elem['lower_bound'])
        experiments[(elem['algorithm'],elem['direction'],elem['num_seed'],"total_couples")].append(elem['total_couples'])
        experiments[(elem['algorithm'],elem['direction'],elem['num_seed'],"memory_used")].append(elem['memory_used'])
        experiments[(elem['algorithm'],elem['direction'],elem['num_seed'],"time")].append(elem['time'])



    # Building a dictionary for the statistics

    for algo in algo_list:
        for direction in direction_list:
            for seed in seed_list:

                statistics[(algo,direction,seed)] = summary(algo,direction,seed)


                statistics[(algo,direction,seed)].SampleMeans(experiments[(algo,direction,seed,"avg_distance")],experiments[(algo,direction,seed,"effective_diameter")],
                                                            experiments[(algo,direction,seed,"lower_bound")],
                                                            experiments[(algo,direction,seed,"total_couples")],experiments[(algo,direction,seed,"memory_used")],
                                                              experiments[(algo, direction, seed, "time")]

                                                            )

                results[(algo,direction,seed,"MeanAvgDistance")] = [statistics[(algo,direction,seed)].get_avgDistanceSampleMean()]
                results[(algo,direction,seed,"MeanEffectiveDiameter")] = [statistics[(algo,direction,seed)].get_effectiveDiameterSampleMean()]
                results[(algo,direction,seed,"MeanLowerBoundDiameter")] = [statistics[(algo,direction,seed)].get_lowerBoundDiameterSampleMean()]
                results[(algo,direction,seed,"MeanTotalCouples")] = [statistics[(algo,direction,seed)].get_totalCouplesSampleMean()]
                results[(algo,direction,seed,"MeanMaxMemoryUsed")] = [statistics[(algo,direction,seed)].get_maxMemoryUsedSampleMean()]
                results[(algo,direction,seed,"avgTime")] = [statistics[(algo,direction,seed)].get_avg_time()]


                if(std):

                    statistics[(algo,direction,seed)].StandardDeviations()

                    results[(algo, direction, seed, "MeanAvgDistance")].append(
                        statistics[(algo, direction, seed)].get_avgDistanceStd())
                    results[(algo, direction, seed, "MeanEffectiveDiameter")].append(
                        statistics[(algo, direction, seed)].get_effectiveDiameterStd())
                    results[(algo, direction, seed, "MeanLowerBoundDiameter")].append(
                        statistics[(algo, direction, seed)].get_lowerBoundDiameterStd())
                    results[(algo, direction, seed, "MeanTotalCouples")].append(
                        statistics[(algo, direction, seed)].get_totalCouplesStd())

                    results[(algo, direction, seed, "MeanMaxMemoryUsed")].append(
                        statistics[(algo, direction, seed)].get_maxMemoryUsedStd())

                    results[(algo, direction, seed, "avgTime")].append(
                        statistics[(algo, direction, seed)].get_std_time())

                if(GroundTruthPath != None):
                    statistics[(algo, direction, seed)].set_all_ground_truth(groundTruth)
                    statistics[(algo, direction, seed)].Residuals()
                    residuals = {}

                    residuals['avg_distance'] = statistics[(algo, direction, seed)].get_avgDistanceResidual()
                    residuals['effective_diameter'] = statistics[(algo, direction, seed)].get_effectiveDiameterResidual()
                    residuals['lower_bound'] = statistics[
                        (algo, direction, seed)].get_lowerBoundDiameterResidual()
                    residuals["total_couples"] = statistics[(algo, direction, seed)].get_totalCouplesResidual()
                    results[(algo, direction, seed, "residuals")] = residuals

                    CI = {}

                    CI['avg_distance'] = statistics[(algo, direction, seed)].get_ciAvgDistance()
                    CI['effective_diameter'] = statistics[(algo, direction, seed)].get_ciEffectiveDiameter()
                    CI['lower_bound'] = statistics[(algo, direction, seed)].get_ciLowerBoundDiameter()
                    CI['total_couples'] = statistics[(algo, direction, seed)].get_ciTotalCouples()
                    results[(algo, direction, seed, "confidence_Interval")] = CI

                    

                if(Ttest):
                    statistics[(algo, direction, seed)].Ttest()
                    statistics[(algo, direction, seed)].wilcoxon_test()
                    results[(algo, direction, seed, "Ttest")] = statistics[(algo, direction, seed)].get_all_Ttests()
                    results[(algo, direction, seed, "Wilcoxon")] = statistics[(algo, direction, seed)].get_all_WilcoxonTest()

    if(dataframe):
        head = ["algo","direction","seeds"]
        if(std):
            if(GroundTruthPath):
                if(Ttest):
                    head.extend(["groundTruthAvgDistance","sampleMeanAvgDistance","stdAvgDistance","residualAvgDistance","pValueAvgDistance","pvalWilcoxonAvgDistance","ciAvgDistance",
                "groundTruthEffectiveDiameter","sampleMeanEffectiveDiameter","stdEffectiveDiameter","residualEffectiveDiameter","pValueEffectiveDiameter","pvalWilcoxonEffectiveDiameter","ciEffectiveDiameter",
                "groundTruthLowerBoundDiameter","sampleMeanLowerBoundDiameter","stdLowerBoundDiameter","residualLowerBoundDiameter","pValueLowerBoundDiameter","pvalWilcoxonLowerBoundDiameter","ciLowerBoundDiameter",
                "groundTruthTotalCouples","sampleMeanTotalCouples","stdTotalCouples","residualTotalCouples","pValueTotalCouples","pvalWilcoxonTotalCouples","CITotalCouples","avg_time","std_time"])
                    elements = []
                    for algo in algo_list:
                        for direction in direction_list:
                            for seed in seed_list:
                                elements.append([
                                    algo, direction, seed, groundTruth['avg_distance'],
                                    results[(algo, direction, seed, "MeanAvgDistance")][0],
                                    results[(algo, direction, seed, "MeanAvgDistance")][1],
                                    results[(algo, direction, seed, "residuals")]["avg_distance"],
                                    results[(algo, direction, seed, "Ttest")]['avg_distance'],
                                    results[(algo, direction, seed, "Wilcoxon")]['avg_distance'],
                                    results[(algo, direction, seed, "confidence_Interval")]['avg_distance'],

                                    groundTruth['effective_diameter'],
                                    results[(algo, direction, seed, "MeanEffectiveDiameter")][0],
                                    results[(algo, direction, seed, "MeanEffectiveDiameter")][1],
                                    results[(algo, direction, seed, "residuals")]["effective_diameter"],
                                    results[(algo, direction, seed, "Ttest")]['effective_diameter'],
                                    results[(algo, direction, seed, "Wilcoxon")]['effective_diameter'],
                                    results[(algo, direction, seed, "confidence_Interval")]['effective_diameter'],

                                    groundTruth['lower_bound'],
                                    results[(algo, direction, seed, "MeanLowerBoundDiameter")][0],
                                    results[(algo, direction, seed, "MeanLowerBoundDiameter")][1],
                                    results[(algo, direction, seed, "residuals")]["lower_bound"],
                                    results[(algo, direction, seed, "Ttest")]['lower_bound'],
                                    results[(algo, direction, seed, "Wilcoxon")]['lower_bound'],
                                    results[(algo, direction, seed, "confidence_Interval")]['lower_bound'],

                                    groundTruth['total_couples'],
                                    results[(algo, direction, seed, "MeanTotalCouples")][0],
                                    results[(algo, direction, seed, "MeanTotalCouples")][1],
                                    results[(algo, direction, seed, "residuals")]["total_couples"],
                                    results[(algo, direction, seed, "Ttest")]['total_couples'],
                                    results[(algo, direction, seed, "Wilcoxon")]['total_couples'],
                                    results[(algo, direction, seed, "confidence_Interval")]['total_couples'],

                                    results[(algo, direction, seed, "avgTime")][0],
                                    results[(algo, direction, seed, "avgTime")][1]
                                ])

                else:
                    head.extend(["groundTruthAvgDistance","sampleMeanAvgDistance","stdAvgDistance","residualAvgDistance",
                "groundTruthEffectiveDiameter","sampleMeanEffectiveDiameter","stdEffectiveDiameter","residualEffectiveDiameter",
                "groundTruthLowerBoundDiameter","sampleMeanLowerBoundDiameter","stdLowerBoundDiameter","residualLowerBoundDiameter",
                "groundTruthTotalCouples","sampleMeanTotalCouples","stdTotalCouples","residualTotalCouples","avg_time","std_time"])
                    elements = []
                    for algo in algo_list:
                        for direction in direction_list:
                            for seed in seed_list:
                                elements.append([
                                    algo, direction, seed, groundTruth['avg_distance'],
                                    results[(algo, direction, seed, "MeanAvgDistance")][0],
                                    results[(algo, direction, seed, "MeanAvgDistance")][1],
                                    results[(algo, direction, seed, "residuals")]["avg_distance"],
                                    groundTruth['effective_diameter'],
                                    results[(algo, direction, seed, "MeanEffectiveDiameter")][0],
                                    results[(algo, direction, seed, "MeanEffectiveDiameter")][1],
                                    results[(algo, direction, seed, "residuals")]["effective_diameter"],
                                    groundTruth['lower_bound'],
                                    results[(algo, direction, seed, "MeanLowerBoundDiameter")][0],
                                    results[(algo, direction, seed, "MeanLowerBoundDiameter")][1],
                                    results[(algo, direction, seed, "residuals")]["lower_bound"],
                                    groundTruth['total_couples'],
                                    results[(algo, direction, seed, "MeanTotalCouples")][0],
                                    results[(algo, direction, seed, "MeanTotalCouples")][1],
                                    results[(algo, direction, seed, "residuals")]["total_couples"],

                                    results[(algo, direction, seed, "avgTime")][0],
                                    results[(algo, direction, seed, "avgTime")][1]
                                ])
            else:
                head.extend([ "sampleMeanAvgDistance", "stdAvgDistance", "residualAvgDistance",
                              "sampleMeanEffectiveDiameter", "stdEffectiveDiameter",
                             "residualEffectiveDiameter",
                             "sampleMeanLowerBoundDiameter", "stdLowerBoundDiameter",
                             "residualLowerBoundDiameter",
                              "sampleMeanTotalCouples", "stdTotalCouples",
                             "residualTotalCouples","avg_time","std_time"])
                elements = []
                for algo in algo_list:
                    for direction in direction_list:
                        for seed in seed_list:
                            elements.append([
                                algo, direction, seed,
                                results[(algo, direction, seed, "MeanAvgDistance")][0],
                                results[(algo, direction, seed, "MeanAvgDistance")][1],
                                results[(algo, direction, seed, "MeanEffectiveDiameter")][0],
                                results[(algo, direction, seed, "MeanEffectiveDiameter")][1],
                                results[(algo, direction, seed, "MeanLowerBoundDiameter")][0],
                                results[(algo, direction, seed, "MeanLowerBoundDiameter")][1],
                                results[(algo, direction, seed, "MeanTotalCouples")][0],
                                results[(algo, direction, seed, "MeanTotalCouples")][1],
                                results[(algo, direction, seed, "avgTime")][0],
                                results[(algo, direction, seed, "avgTime")][1]
                            ])
        else:
            if (GroundTruthPath):
                if (Ttest):
                    head.extend(
                        ["groundTruthAvgDistance", "sampleMeanAvgDistance", "residualAvgDistance",
                         "pValueAvgDistance",
                         "groundTruthEffectiveDiameter", "sampleMeanEffectiveDiameter",
                         "residualEffectiveDiameter", "pValueEffectiveDiameter",
                         "groundTruthLowerBoundDiameter", "sampleMeanLowerBoundDiameter",
                         "residualLowerBoundDiameter", "pValueLowerBoundDiameter",
                         "groundTruthTotalCouples", "sampleMeanTotalCouples", "residualTotalCouples",
                         "pValueTotalCouples","avg_time","std_time"])
                    elements = []
                    for algo in algo_list:
                        for direction in direction_list:
                            for seed in seed_list:
                                elements.append([
                                    algo, direction, seed, groundTruth['avg_distance'],
                                    results[(algo, direction, seed, "MeanAvgDistance")][0],
                                    results[(algo, direction, seed, "residuals")]["avg_distance"],
                                    results[(algo, direction, seed, "Ttest")]['avg_distance'],
                                    groundTruth['effective_diameter'],
                                    results[(algo, direction, seed, "MeanEffectiveDiameter")][0],
                                    results[(algo, direction, seed, "residuals")]["effective_diameter"],
                                    results[(algo, direction, seed, "Ttest")]['effective_diameter'],
                                    groundTruth['lower_bound'],
                                    results[(algo, direction, seed, "MeanLowerBoundDiameter")][0],
                                    results[(algo, direction, seed, "residuals")]["lower_bound"],
                                    results[(algo, direction, seed, "Ttest")]['lower_bound'],
                                    groundTruth['total_couples'],
                                    results[(algo, direction, seed, "MeanTotalCouples")][0],
                                    results[(algo, direction, seed, "residuals")]["total_couples"],
                                    results[(algo, direction, seed, "Ttest")]['total_couples'],

                                    results[(algo, direction, seed, "avgTime")][0],
                                    results[(algo, direction, seed, "avgTime")][1]

                                ])

                else:
                    head.extend(
                        ["groundTruthAvgDistance", "sampleMeanAvgDistance", "residualAvgDistance",
                         "groundTruthEffectiveDiameter", "sampleMeanEffectiveDiameter",
                         "residualEffectiveDiameter",
                         "groundTruthLowerBoundDiameter", "sampleMeanLowerBoundDiameter",
                         "residualLowerBoundDiameter",
                         "groundTruthTotalCouples", "sampleMeanTotalCouples",
                         "residualTotalCouples","avg_time","std_time"])
                    elements = []
                    for algo in algo_list:
                        for direction in direction_list:
                            for seed in seed_list:
                                elements.append([
                                    algo, direction, seed, groundTruth['avg_distance'],
                                    results[(algo, direction, seed, "MeanAvgDistance")][0],
                                    results[(algo, direction, seed, "residuals")]["avg_distance"],
                                    groundTruth['effective_diameter'],
                                    results[(algo, direction, seed, "MeanEffectiveDiameter")][0],
                                    results[(algo, direction, seed, "residuals")]["effective_diameter"],
                                    groundTruth['lower_bound'],
                                    results[(algo, direction, seed, "MeanLowerBoundDiameter")][0],
                                    results[(algo, direction, seed, "residuals")]["lower_bound"],
                                    groundTruth['total_couples'],
                                    results[(algo, direction, seed, "MeanTotalCouples")][0],
                                    results[(algo, direction, seed, "residuals")]["total_couples"],

                                    results[(algo, direction, seed, "avgTime")][0],
                                    results[(algo, direction, seed, "avgTime")][1]
                                ])
            else:
                head.extend(["sampleMeanAvgDistance",
                             "sampleMeanEffectiveDiameter",

                             "sampleMeanLowerBoundDiameter",

                             "sampleMeanTotalCouples",
                             "avg_time",
                             "std_time"
                             ])
                elements = []

                for algo in algo_list:
                    for direction in direction_list:
                        for seed in seed_list:

                            elements.append([
                                algo, direction, seed,
                                results[(algo, direction, seed, "MeanAvgDistance")][0],
                                results[(algo, direction, seed, "MeanEffectiveDiameter")][0],
                                results[(algo, direction, seed, "MeanLowerBoundDiameter")][0],
                                results[(algo, direction, seed, "MeanTotalCouples")][0],
                                results[(algo, direction, seed, "avgTime")][0],
                                results[(algo, direction, seed, "avgTime")][1]

                            ])




        df = pd.DataFrame(elements,columns=head)
        df.round(rounding)
        df.to_csv(OutputPath+'.csv')

        if((LabelPath!= None) and (Ttest)):
            try:
                with open(LabelPath) as json_file:
                    newLabels = json.load(json_file)
            except IOError:
                print('Error! Can not open '+ LabelPath)
            #print(head)

            header =[]
            for elem in head:
                if(elem in newLabels.keys()):
                    header.append(elem)

            # The set operation does not preserve the original order.
            #header = list(set(head).intersection(set(newLabels.keys())))

            newHeads = []
            for name in header:
                newHeads.append(newLabels[name])


            relabeled = []
            for algo in algo_list:
                table = []
                for direction in direction_list:
                    for seed in seed_list:
                        table.append([
                            direction, seed,
                            groundTruth['avg_distance'],results[(algo, direction, seed,"MeanAvgDistance")][0],
                            str(results[(algo, direction, seed,"residuals")]["avg_distance"])+" " +"("+str(results[(algo, direction, seed,"Ttest")]['avg_distance'])+")",
                            "+ - "+str(results[(algo, direction, seed, "confidence_Interval")]['avg_distance']),

                            groundTruth['effective_diameter'], results[(algo, direction, seed, "MeanEffectiveDiameter")][0],
                            str(results[(algo, direction, seed, "residuals")]["effective_diameter"]) + " " +"(" + str(
                                results[(algo, direction, seed, "Ttest")]['effective_diameter']) + ")",
                            "+ - " + str(results[(algo, direction, seed, "confidence_Interval")]['effective_diameter']),

                            groundTruth['lower_bound'], results[(algo, direction, seed, "MeanLowerBoundDiameter")][0],
                            str(results[(algo, direction, seed, "residuals")]["lower_bound"]) + " " +"(" + str(
                                results[(algo, direction, seed, "Ttest")]['lower_bound']) + ")",
                            "+ - " + str(results[(algo, direction, seed, "confidence_Interval")]['lower_bound']),

                            groundTruth['total_couples'], results[(algo, direction, seed, "MeanTotalCouples")][0],
                            str(results[(algo, direction, seed, "residuals")]["total_couples"])+" " + "(" + str(
                                results[(algo, direction, seed, "Ttest")]['total_couples']) + ")",
                            "+ - " + str(results[(algo, direction, seed, "confidence_Interval")]['total_couples']),

                            results[(algo, direction, seed, "avgTime")][0],
                            results[(algo, direction, seed, "avgTime")][1],


                        ])
                relabeled.append(table)


            i = 0

            for tab in relabeled:

                df_rel = pd.DataFrame(tab, columns=newHeads)
                df_rel.round(rounding)
                df_rel.to_csv(OutputPath + str(algo_list[i]) + '.csv')
                i+=1


    dict = {}
    for k,v in results.items():
        newkey= str()
        c = 0
        for elem in k:

            if(isinstance(elem,int)):

                newkey += str(elem)
            else:
                newkey += elem
            if(c<len(k)):
                newkey += ":"
            c+=1

        dict[newkey] = v



    with open(OutputPath+'.json', 'w') as outfile:
        json.dump(dict, outfile)

def main(argv):
   inputfile = ''
   outputfile = ''
   GroundTruthPath = None
   std = True
   Ttest = False
   df = False
   relabel = ''
   rounding = 5

   try:
      opts, args = getopt.getopt(argv,"hi:o:g:s:t:d:l:r:",["ifile=","ofile=","gfile=","std=","Ttest=","DF=","LAB=","ROUND="])
   except getopt.GetoptError:
        print("Error")
        sys.exit(2)
   for opt, arg in opts:
      if opt == '-h':
         print ('test.py -i <inputfile> -o <outputfile>')
         sys.exit()
      elif opt in ("-i", "--ifile"):
         inputfile = arg
      elif opt in ("-o", "--ofile"):
         outputfile = arg
      elif opt in ("-g","--gfile"):
          GroundTruthPath = arg
      elif opt in ("-s","--std"):
          if(arg in "-False"):
              std = False
          elif(arg in "-True"):
              std= True
      elif opt in ("-t","--Ttest"):

          if(arg in "-True" ):
              Ttest = True
          elif(arg in "-False"):
              Ttest = False
      elif opt in ("-d","--DF"):
          if (arg in "-True"):
              df = True
          elif (arg in "-False"):
              df = False
      elif opt in ("-l", "--LAB"):
          relabel = arg
      elif opt in ("-r","--ROUND"):
          rounding =int(arg)
   read_json(inputfile, outputfile, GroundTruthPath, std=std, Ttest=Ttest,dataframe=df,LabelPath=relabel,rounding = rounding)


if __name__ == "__main__":
   main(sys.argv[1:])

# Relabeling feature: DEMO version

# Input = "/Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/Parsati/worldSeries"
# Out = ""
# GroundTruthPath = "/Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/groundTruths/worldSeriesGT.json"
#
# read_json(Input,Out,GroundTruthPath,std = True,Ttest = True)
# Esempio di esecuzione
#  python readJson.py -i /Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/Parsati/worldSeriesRetweets -o /Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/worldSeriesRetweetsStats -g /Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/groundTruths/worldSeriesGT.json -s True -t True -d True -l /Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/TableRelabeling/relabel.json


