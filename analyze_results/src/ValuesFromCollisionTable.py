import json
import sys, getopt
from src.objects.stats import resultsStats

def ValuesFromCollisionTable(InputPath,OutputPath,AdditionalInfoPath = None,SeedList = [256]):

    try:
        with open(InputPath) as json_file:
            data = json.load(json_file)
    except IOError:
        print('Error! Can not open '+InputPath)

    if(AdditionalInfoPath != None):
        try:
            with open(AdditionalInfoPath) as json_file:
                AdditionalInfos = json.load(json_file)
        except IOError:
            print('Error! Can not open ' + AdditionalInfoPath)
    resultList = []
    i = 0
    for seed in SeedList:
        for elem in data:
            #print("Test numero = ", i)
            hoptabProva = resultsStats(elem["collision_table"], elem['last_hops'], elem['nodes'],elem['node_ids'],elem['seeds_time'], seed=seed)

            i+=1
            hoptabProva.printStats()
            additionalDict = {}
            for name in AdditionalInfos.values():
                additionalDict[name] = elem[name]



            resultList.append(hoptabProva.get_stats(additionalDict))

    with open(OutputPath + '.json', 'w') as outfile:
        json.dump(resultList, outfile)


def main(argv):
   inputfile = ''
   outputfile = ''
   additionalFile = None
   seeds = []
   try:
      opts, args = getopt.getopt(argv,"hi:o:a:s:",["ifile=","ofile=","afile=","seeds="])
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
      elif opt in ("-a","--gfile"):
         additionalFile = arg
      elif opt in ("-s","--seeds"):
         splittedInput = arg.split(",")
         for seed in splittedInput:
             seeds.append(int(seed))
   if(not seeds):
        seeds.append(256)

   ValuesFromCollisionTable(inputfile,outputfile,additionalFile,SeedList=seeds)


if __name__ == "__main__":
   main(sys.argv[1:])

#InputPath = "/Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/Parsati/worldSeriesRetweets"
#OutputPath = "/Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/Transformed/worldSeriesRetweets"
#AdditionalInfoPath = "/Users/antoniocruciani/Dropbox/EsperimentiDaAnalizzareFUB/additionalInfos/addInfos.json"
#ValuesFromHopTable(InputPath,OutputPath,AdditionalInfoPath)



# python ValuesFromCollisionTable.py -i /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test10Run_GroundTruth_vs_BMH256/10run/with_iso/dblp-2010.json -o /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test10Run_GroundTruth_vs_BMH256/Analizzati/Calcolati/iso -a /Users/antoniocruciani/Dropbox/MHSEDataAnalysisTool/additionalInfos/addInfos.json -s 16,32,64,128,256

# python ValuesFromCollisionTable.py -i /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test100Run_GroundTruth_vs_BMH256/100run/with_iso/dblp-2010.json -o /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test100Run_GroundTruth_vs_BMH256/100run/with_iso/dblp -a /Users/antoniocruciani/Dropbox/MHSEDataAnalysisTool/additionalInfos/addInfos.json -s 16,32,64,128,256
# python readJson.py -i /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test100Run_GroundTruth_vs_BMH256/100run/with_iso/dblp.json -o /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test10Run_GroundTruth_vs_BMH256/Analizzati/Calcolati/dblp-2010 -g /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test10Run_GroundTruth_vs_BMH256/groundtruth/dblp-2010/with_iso/gt_dblp-2010_with_iso.json -s True -t True -d True -l /Users/antoniocruciani/Dropbox/MHSEDataAnalysisTool/TableRelabeling/relabel.json

 # python readJson.py -i /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test10Run_GroundTruth_vs_BMH256/Analizzati/Calcolati/iso/dblp-2010.json -o /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test10Run_GroundTruth_vs_BMH256/Analizzati/Calcolati/iso/dblp-2010 -g /Users/antoniocruciani/Dropbox/FUB-IIR/Esperimenti_Nov2020/Test10Run_GroundTruth_vs_BMH256/groundtruth/dblp-2010/with_iso/gt_dblp-2010_with_iso.json -s True -t True -d True -l /Users/antoniocruciani/Dropbox/MHSEDataAnalysisTool/TableRelabeling/relabel.jso
