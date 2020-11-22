package it.bigdatalab.model;

public class Parameter {
    private final String mInputFilePathGraph;
    private final String mOutputFolderPath;
    private final String mAlgorithmName;
    private final int mNumTests;

    private final int mNumSeeds;
    private final boolean mIsSeedsRandom;
    private final boolean mAutomaticRange;
    private final String mInputFilePathSeed;
    private final String mInputFilePathNodes;
    private final int[] mRange;

    private final boolean mIsolatedVertices;
    private final String mDirection;
    private final boolean mTranspose;

    private final boolean mInMemory;

    private final double mThreshold;

    private final int mNumThreads;

    @org.jetbrains.annotations.Contract(pure = true)
    public Parameter(Builder builder) {
        this.mInputFilePathGraph = builder.inputFilePathGraph;
        this.mOutputFolderPath = builder.outputFolderPath;
        this.mAlgorithmName = builder.algorithmName;
        this.mNumTests = builder.numTests;

        this.mNumSeeds = builder.numSeeds;
        this.mIsSeedsRandom = builder.isSeedsRandom;
        this.mAutomaticRange = builder.automaticRange;

        this.mInputFilePathSeed = builder.inputFilePathSeed;
        this.mInputFilePathNodes = builder.inputFilePathNodes;
        this.mRange = builder.range;

        this.mIsolatedVertices = builder.isolatedVertices;
        this.mDirection = builder.direction;
        this.mTranspose = builder.transpose;

        this.mInMemory = builder.inMemory;
        this.mThreshold = builder.threshold;

        this.mNumThreads = builder.numThreads;
    }

    public String getInputFilePathGraph() {
        return mInputFilePathGraph;
    }

    public String getOutputFolderPath() {
        return mOutputFolderPath;
    }

    public String getAlgorithmName() {
        return mAlgorithmName;
    }

    public int getNumTests() {
        return mNumTests;
    }

    public int getNumSeeds() {
        return mNumSeeds;
    }

    public boolean isSeedsRandom() {
        return mIsSeedsRandom;
    }

    public boolean isAutomaticRange() {
        return mAutomaticRange;
    }

    public String getInputFilePathSeed() {
        return mInputFilePathSeed;
    }

    public String getInputFilePathNodes() {
        return mInputFilePathNodes;
    }

    public int[] getRange() {
        return mRange;
    }

    public boolean keepIsolatedVertices() {
        return mIsolatedVertices;
    }

    public String getDirection() {
        return mDirection;
    }

    public boolean isTranspose() {
        return mTranspose;
    }

    public boolean isInMemory() {
        return mInMemory;
    }

    public double getThreshold() {
        return mThreshold;
    }

    public int getNumThreads() {
        return mNumThreads;
    }

    public static class Builder {
        private String inputFilePathGraph;
        private String outputFolderPath;
        private String algorithmName;
        private int numTests;

        private int numSeeds;
        private boolean isSeedsRandom;
        private boolean automaticRange;
        private String inputFilePathSeed;
        private String inputFilePathNodes;
        private int[] range;

        private boolean isolatedVertices;
        private String direction;
        private boolean transpose;

        private boolean inMemory;
        private double threshold;

        private int numThreads;

        public Builder() {
        }

        public Builder setInputFilePathGraph(String inputFilePathGraph) {
            this.inputFilePathGraph = inputFilePathGraph;
            return this;
        }

        public Builder setOutputFolderPath(String outputFolderPath) {
            this.outputFolderPath = outputFolderPath;
            return this;
        }

        public Builder setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
            return this;
        }

        public Builder setNumTests(int numTests) {
            this.numTests = numTests;
            return this;
        }

        public Builder setNumSeeds(int numSeeds) {
            this.numSeeds = numSeeds;
            return this;
        }

        public Builder setSeedsRandom(boolean isSeedsRandom) {
            this.isSeedsRandom = isSeedsRandom;
            return this;
        }

        public Builder setAutomaticRange(boolean automaticRange) {
            this.automaticRange = automaticRange;
            return this;
        }

        public Builder setInputFilePathSeed(String inputFilePathSeed) {
            this.inputFilePathSeed = inputFilePathSeed;
            return this;
        }

        public Builder setInputFilePathNodes(String inputFilePathNodes) {
            this.inputFilePathNodes = inputFilePathNodes;
            return this;
        }

        public Builder setRange(int[] range) {
            this.range = range;
            return this;
        }

        public Builder setIsolatedVertices(boolean isolatedVertices) {
            this.isolatedVertices = isolatedVertices;
            return this;
        }

        public Builder setDirection(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder setTranspose(boolean transpose) {
            this.transpose = transpose;
            return this;
        }

        public Builder setInMemory(boolean inMemory) {
            this.inMemory = inMemory;
            return this;
        }

        public Builder setThreshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder setNumThreads(int numThreads) {
            this.numThreads = numThreads;
            return this;
        }

        public Parameter build() {
            return new Parameter(this);
        }
    }
}
