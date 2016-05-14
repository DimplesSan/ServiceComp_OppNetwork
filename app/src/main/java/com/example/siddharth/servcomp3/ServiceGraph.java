package com.example.siddharth.servcomp3;


import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class ServiceGraph implements Serializable, Parcelable {


    public ArrayList<Service> listOfServices;
    public LinkedHashMap<Service, LinkedHashSet<Service>> mapOfHighLevelServiceToComposition;
    public ArrayList<Service> composedServiceRepresentations;       //List of high level representations of the composed services

    private ArrayList<LinkedHashSet<Service>> listofComposedServices;   //List of composed services
    private LinkedHashMap<String, ArrayList<Service>> ioDescriptionNodes;   //Adjacency list representing the Semantic graph


    //Default Constructor
    public ServiceGraph(){
        listOfServices = new ArrayList<Service>();
        composedServiceRepresentations = new ArrayList<Service>();
        mapOfHighLevelServiceToComposition = new LinkedHashMap<Service, LinkedHashSet<Service>>();
    }

    //Constructor to initialize with a list of services
    public ServiceGraph(ArrayList<Service> _listOfServices){

        listOfServices = _listOfServices;   //Initialize the list of services
        composedServiceRepresentations = new ArrayList<Service>();
        mapOfHighLevelServiceToComposition = new LinkedHashMap<Service, LinkedHashSet<Service>>();

        ioDescriptionNodes = computeSemanticGraph(listOfServices);

        //From the semantic graph get the list of all possible paths
        //and then Filter the semantic paths based on syntactic types
        listofComposedServices = filterSetOfComposedServices(getAllSemanticPaths(ioDescriptionNodes, true));
        setComposedServiceRepresentation();
    }

    //Add a single service to the list of services
    public void addAService(Service s){

        this.listOfServices.add(s);

        //Recompute the following
        ioDescriptionNodes = computeSemanticGraph(listOfServices);

        //From the semantic graph get the list of all possible paths
        //and then Filter the semantic paths based on syntactic types
        listofComposedServices = filterSetOfComposedServices(getAllSemanticPaths(ioDescriptionNodes, true));
        setComposedServiceRepresentation();
    }

    //Method to update both graphs based on the incoming list of services
    public void updateServiceGraph(ServiceGraph tmpServGraph){

//        this.listOfServices.addAll(tmpServGraph.listOfServices);
        for(Service s : tmpServGraph.listOfServices){
            if(!this.listOfServices.contains(s))
                this.listOfServices.add(s);
        }

        //Recompute the following
        ioDescriptionNodes = computeSemanticGraph(listOfServices);

        //From the semantic graph get the list of all possible paths
        //and then Filter the semantic paths based on syntactic types
        listofComposedServices = filterSetOfComposedServices(getAllSemanticPaths(ioDescriptionNodes, true));
        setComposedServiceRepresentation();
    }


    public void refreshServiceGraph(){

        //Clear all the queues
        mapOfHighLevelServiceToComposition.clear();
        composedServiceRepresentations.clear();
        listofComposedServices.clear();
        ioDescriptionNodes.clear();

        //Recompute the following
        ioDescriptionNodes = computeSemanticGraph(listOfServices);

        //From the semantic graph get the list of all possible paths
        //and then Filter the semantic paths based on syntactic types
        listofComposedServices = filterSetOfComposedServices(getAllSemanticPaths(ioDescriptionNodes, true));
        setComposedServiceRepresentation();
    }

    //Method to get the high level composed services
    private void setComposedServiceRepresentation(){

        for(LinkedHashSet<Service> composedServices: listofComposedServices){
            ArrayList<Service> arrServices = new ArrayList<Service>(composedServices);
            Service lastService = arrServices.get(arrServices.size()-1);
            Service firstService = arrServices.get(0);

            //Combine the services for a high level representation only if the input and output are not the same
            if(!(firstService.inputDesc.equalsIgnoreCase(lastService.outputDesc) &&
                    firstService.inputFileType.equalsIgnoreCase(lastService.outputFileType)
            )
                    )
            {
                String[] params = {firstService.inputDesc, lastService.outputDesc,
                        firstService.inputFilePath, lastService.outputFilePath,
                        firstService.inputFileType, lastService.outputFileType,
                        "ComposedService from " + firstService.inputDesc + " to " +
                                lastService.outputDesc,
                        "Multiple devices"
                };
                Service tempService = new Service(params);

                //A check to see if the service is already added to the list or not
                if(!this.composedServiceRepresentations.contains(tempService)) {
                    this.composedServiceRepresentations.add(tempService);
                    this.mapOfHighLevelServiceToComposition.put(tempService, composedServices); //Add it to the map for later retrieval
                }
            }

        }
    }

    //---------------------------------------Internal Methods----------------------------------------------------------
    //Method to compute semantic graph
    private LinkedHashMap<String, ArrayList<Service>> computeSemanticGraph(ArrayList<Service>  lst){

        //Create a blank adj list representing the semantic graph
        LinkedHashMap<String, ArrayList<Service>>  tempGraph = new LinkedHashMap<String,ArrayList<Service>>();

        //For each get the ip lst the op desc and add them to the list
        for(Service s : listOfServices){

            //Add the ip and the op nodes
            if(! tempGraph.containsKey(s.inputDesc))
                tempGraph.put( s.inputDesc, new ArrayList<Service>());

            if(! tempGraph.containsKey(s.outputDesc))
                tempGraph.put(s.outputDesc, new ArrayList<Service>());
        }

        //Add the service object that represents an edge in the service graph
        for(Service s : lst)
            tempGraph.get(s.inputDesc).add(s); // Add the output node to the list corresponding to the ipnode


        return tempGraph;  // Return the Adjacency list
    }



    //Method to compute the syntactic graph
//    private LinkedHashMap<String, ArrayList<Service>> computeSyntacticGraph(ArrayList<Service>  lst){
//
//       //Create a blank adj list representing the semantic graph
//        LinkedHashMap<String, ArrayList<Service>>  tempGraph = new LinkedHashMap<String,ArrayList<Service>>();
//
//        //For each get the ip and the op desc and add them to the list
//        for(Service s : lst){
//
//            //Add the ip and the op nodes
//            if(! tempGraph.containsKey(s.inputFileType))
//                tempGraph.put( s.inputFileType, new ArrayList<Service>());
//
//            if(! tempGraph.containsKey(s.outputFileType))
//                tempGraph.put(s.outputFileType, new ArrayList<Service>());
//        }
//
//        //Add the service object that represents an edge in the service graph
//        for(Service s : lst)
//            tempGraph.get(s.inputFileType).add(s); // Add the output node to the list corresponding to the ipnode
//
//
//        return tempGraph;  // Return the Adjacency list
//    }




    private ArrayList<LinkedHashSet<Service>> getAllSemanticPaths(LinkedHashMap<String, ArrayList<Service>> graph,
                                                                  boolean isSemantic){

        ArrayList<LinkedHashSet<Service>> listOFPaths = new ArrayList<LinkedHashSet<Service>>();

        //Find longest path for every node in the graph
        for(String ipNode : graph.keySet())
            getAllPathsFromANode(graph, ipNode, listOFPaths, isSemantic);

        return listOFPaths;
    }



    private void getAllPathsFromANode(LinkedHashMap<String, ArrayList<Service>> graph, String startNode,
                                      ArrayList<LinkedHashSet<Service>> _listOFPaths,
                                      boolean isSemanticPathSearch){

        //Get the list of nodes adjacent to the start node
        ArrayList<Service> listOfAdjNodes = graph.get(startNode);
        ArrayList<LinkedHashSet<Service>> listOFPaths = _listOFPaths;

        if(listOfAdjNodes.size() > 0) {

            LinkedHashSet <Service>path = null;
            for(Service s : listOfAdjNodes){

                //Create a new linked hash set to keep track of the services encountered
                path = new LinkedHashSet<Service>();

                //Add current service to the set of encountered services
                path.add(s);

                //Do a DFS search
                if(isSemanticPathSearch)
                    dfsPathSearch(graph, s.outputDesc, path, isSemanticPathSearch);
                else {
                    dfsPathSearch(graph, s.outputFileType, path, isSemanticPathSearch);
                }

                //Add all the encountered services to the arraylist
                listOFPaths.add(path);
            }


        }
    }




    private void dfsPathSearch(LinkedHashMap<String, ArrayList<Service>> graph,
                               String node, LinkedHashSet<Service> encounteredServices,
                               boolean isSemanticSearch){

        //If there are no outgoing edges then return
        if(graph.get(node).size() == 0)
            return;
        else {

            //If there are outgoing edges then check for each outgoing edge
            for (Service s : graph.get(node)) {

                //If edge/Service is already encountered. Return - as a cycle is encountered
                if (encounteredServices.contains(s))
                    return;
                else {
                    //Add the service to the set of enountered services
                    encounteredServices.add(s);
                    //
                    if (isSemanticSearch)
                        dfsPathSearch(graph, s.outputDesc, encounteredServices, isSemanticSearch);    //Recursively check
                    else
                        dfsPathSearch(graph, s.outputFileType, encounteredServices, isSemanticSearch);    //Recursively check
                }
            }
        }
    }




    //Filter the Set of services composed on semantic types using the corresponding on syntactic types
    private ArrayList<LinkedHashSet<Service>> filterSetOfComposedServices(ArrayList<LinkedHashSet<Service>> listofSetsWithServices){

        ArrayList<LinkedHashSet<Service>> syntacticCompServices = new ArrayList<LinkedHashSet<Service>>();

        for(LinkedHashSet lHSet : listofSetsWithServices ) {

            //Compute only if the number of services in the set is more than 1
            if(lHSet.size() > 1){

                computeCompServicesFromSemGraphServices(lHSet, syntacticCompServices);
                System.out.println(syntacticCompServices);
            }
        }

        return syntacticCompServices;
    }



    private void computeCompServicesFromSemGraphServices(LinkedHashSet<Service> semanticGraphServices,
                                                         ArrayList<LinkedHashSet<Service>> syntacticCompServices){

        ArrayList<Service> listOFServices =
                new ArrayList<Service>(semanticGraphServices); //Get the start service --it's fixed starting point

        Service startService = listOFServices.get(0);
        listOFServices.remove(startService);//Remove the start service


        LinkedHashSet<Service> tempSet;
        Service currService;
        boolean isCompositionPossible =  true;
        ArrayList<Service> listOfLeftOverServices;

        while(isCompositionPossible){

            currService = startService;
            tempSet = new LinkedHashSet<Service>(); //Set to hold composable services
            listOfLeftOverServices = new ArrayList<Service>();

            tempSet.add(startService);  //Add the start service

            //For every remaining service
            for(Service s : listOFServices){

                //Check semantic and syntactic types
                if(currService.outputDesc.equalsIgnoreCase(s.inputDesc) &&
                        currService.outputFileType.equalsIgnoreCase(s.inputFileType)){

                    tempSet.add(s); //Add the service to hashset
                    currService = s; //Set of composable services
                }
                else
                    listOfLeftOverServices.add(s);


            }

            if(listOfLeftOverServices. size() == listOFServices.size()) //If after iterating all elements the size doesn't reduce then
                isCompositionPossible = false; //stop the iteration

            //Add only if the number of services > 1
            if(tempSet.size() > 1)
                syntacticCompServices.add(tempSet);

            listOFServices =  listOfLeftOverServices;   //Re initialize the leftover services

        }
    }

    //--------------- Parcelable specific code ---------------
    protected ServiceGraph(Parcel in) {
        listOfServices = in.createTypedArrayList(Service.CREATOR);
    }

    public static final Creator<ServiceGraph> CREATOR = new Creator<ServiceGraph>() {
        @Override
        public ServiceGraph createFromParcel(Parcel in) {
            return new ServiceGraph(in);
        }

        @Override
        public ServiceGraph[] newArray(int size) {
            return new ServiceGraph[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(listOfServices);
    }
}