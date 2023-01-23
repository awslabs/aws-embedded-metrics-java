package software.amazon.cloudwatchlogs.emf.serializers;

import software.amazon.cloudwatchlogs.emf.model.StorageResolution;

public class StorageResolutionFilter {

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof StorageResolution)) {
            return false;
        }
        return (obj.toString().equals("STANDARD"));
    }
}
