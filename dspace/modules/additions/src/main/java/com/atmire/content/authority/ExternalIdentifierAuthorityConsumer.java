package com.atmire.content.authority;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by dylan on 30/06/16.
 */
public class ExternalIdentifierAuthorityConsumer implements Consumer
{

    private static Logger log = Logger.getLogger(ExternalIdentifierAuthorityConsumer.class);

    private Set<Integer> itemsToProcess = new HashSet<>();

    private String authorityFields = ConfigurationManager.getProperty("authority.externalidentifier.field");


    public void initialize() throws Exception
    {
        itemsToProcess.clear();
    }

    public void consume(Context context, Event event) throws Exception
    {
        DSpaceObject dso = event.getSubject(context);
        if(dso instanceof Item) {
            itemsToProcess.add(dso.getID());
        }
    }


    /**
     * Commit here.
     * @param context
     * @throws Exception
     */
    public void end(Context context) throws Exception
    {
        try {
            for (Integer itemId : itemsToProcess) {
                Item item = Item.find(context, itemId);
                String[] configuredFields = authorityFields.split(",");

                for (String authorityField : configuredFields) {
                    updateAuthorityValueBasedOnField(item, authorityField);
                }
                item.update();
            }

            context.getDBConnection().commit();

        } finally {
            //Make sure we always clear the list of items to process so that we don't process old items on the next update
            itemsToProcess.clear();
        }
    }

    private void updateAuthorityValueBasedOnField(Item item, String authorityField) {
        Metadatum[] identifierValues = item.getMetadataByMetadataString(authorityField.trim());
        if(ArrayUtils.isNotEmpty(identifierValues)) {
            for (Metadatum identifierValue : identifierValues) {
                if(StringUtils.isBlank(identifierValue.authority)) {
                    Metadatum newValue = identifierValue.copy();
                    newValue.authority = newValue.value;
                    item.replaceMetadataValue(identifierValue, newValue);
                }
            }
        }
    }

    public void finish(Context ctx) throws Exception
    {

    }
}
