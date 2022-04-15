import configparser
import logging
from src.ValuesFromCollisionTable import ValuesFromCollisionTable
from src.readJson import read_json

logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.DEBUG)

config = configparser.ConfigParser()
config.read('config.ini')


if config['VALUES_FROM_COLLISION_TABLE']['input_file']:

    input_file = config['VALUES_FROM_COLLISION_TABLE']['input_file']

    if config['VALUES_FROM_COLLISION_TABLE']['seed_number']:
        seed_number = [int(s) for s in config['VALUES_FROM_COLLISION_TABLE']['seed_number'].split(",")]
    else:
        logging.info("No seed number given, assuming: 16,32,64,128,256")
        seed_number = [16,32,64,128,256]

    if config['VALUES_FROM_COLLISION_TABLE']['output_folder']:
        output_folder =  config['VALUES_FROM_COLLISION_TABLE']['output_folder']

    else:
        logging.info("No input folder given, assuming ./outputs/out_from_collision/outCollision")
        output_folder="./outputs/out_from_collision/outCollision"
        
    if config['VALUES_FROM_COLLISION_TABLE']['additional_information']:
        add_infos = config['VALUES_FROM_COLLISION_TABLE']['additional_information']
    else:
        logging.info("No input folder given, assuming ./additional_info/addInfos.json")
        add_infos ="./additional_info/addInfos.json"

    ValuesFromCollisionTable(input_file, output_folder, add_infos, SeedList=seed_number)

elif config['COMPUTE_RESULTS']['input_file_estimation']:


    if config['COMPUTE_RESULTS']['input_file_exact_measures']:
        input_file_exact = config['COMPUTE_RESULTS']['input_file_exact_measures']
    else:
        logging.error("No EXACT MEASURES GIVEN")
        exit(1)

    if config['COMPUTE_RESULTS']['input_file_estimation']:
        input_file_estimation = config['COMPUTE_RESULTS']['input_file_estimation']
    else:
        logging.error("No estimation file given")
        exit(1)
    if config['COMPUTE_RESULTS']['output_folder']:
        output_folder = config['COMPUTE_RESULTS']['output_folder']

    else:
        logging.info("No input file name given, assuming ./outResults")
        output_folder = "./outResults"

    if config['COMPUTE_RESULTS']['label_path']:
        relable_tab = config['COMPUTE_RESULTS']['label_path']

    else:
        logging.info("No input file name given, assuming ./table_relabeling/relabel.json")
        relable_tab = "./table_relabeling/relabel.json"

    read_json(input_file_estimation, output_folder, input_file_exact, std=True, Ttest=True, dataframe=True, LabelPath=relable_tab,
              rounding=5)
else:
    logging.error("You must choose something to do ")
    exit(1)