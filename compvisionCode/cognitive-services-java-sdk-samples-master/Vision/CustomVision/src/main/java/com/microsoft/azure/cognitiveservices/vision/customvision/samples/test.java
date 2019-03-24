
package com.microsoft.azure.cognitiveservices.vision.customvision.samples;

import java.util.UUID;

import com.google.common.io.ByteStreams;

import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.ImagePrediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.Prediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.PredictionEndpoint;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.CustomVisionPredictionManager;

public class test {
    private static byte[] GetImage(String folder, String fileName)
    {
        try {
            return ByteStreams.toByteArray(CustomVisionSamples.class.getResourceAsStream(folder + "/" + fileName));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args){
// load test image
        byte[] testImage = GetImage("/Test", "test_stop.jpg");
        String predictionApiKey = "83e61e51e77d473e854cfe5c8c075f08";
        UUID project = UUID.fromString("1169609e-6ce9-4f6e-979c-3cafab59e452");
        PredictionEndpoint predictor = CustomVisionPredictionManager.authenticate(predictionApiKey);

// predict
        ImagePrediction results = predictor.predictions().predictImage()
                .withProjectId(project)
                .withImageData(testImage)
                .execute();

        for (Prediction prediction: results.predictions())
        {
            System.out.println(String.format("\t%s: %.2f%%", prediction.tagName(), prediction.probability() * 100.0f));
        }

    }
}
