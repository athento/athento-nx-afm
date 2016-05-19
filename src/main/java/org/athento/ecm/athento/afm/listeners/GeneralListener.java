package org.yerbabuena.ecm.athento.afm.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.yerbabuena.athento.ecm.utils.AthentoNXUtils;

public class GeneralListener
  implements EventListener
{
  private static final Log log = LogFactory.getLog(GeneralListener.class);
  private static final String GENERAL_LISTENER = "generalService";
  private static final String CAPTURED_EVENT = "documentCaptured";
  static boolean busy = false;
  
  public void handleEvent(Event event)
    throws ClientException
  {
    if (event.getName().equals("generalService")) {
      try
      {
        String numberOfFoldersProperty = Framework.getProperty("AFM.NumberOfFolders");
        int numberOfFolders = Integer.parseInt(numberOfFoldersProperty);
        log.info("AFM.NumberOfFolders: " + numberOfFolders);
        for (int folderNumber = 1; folderNumber <= numberOfFolders; folderNumber++)
        {
          String path = Framework.getProperty("AFM.PathDocuments." + folderNumber);
          log.info("AFM.PathDocuments." + folderNumber + ": " + path);
          
          String pathWorkspace = Framework.getProperty("AFM.PathWorkspace." + folderNumber);
          log.info("AFM.PathWorkspace." + folderNumber + ": " + pathWorkspace);
          
          File directorio = new File(path);
          String[] ficheros = directorio.list();
          
          log.info("Number of Documents in folder: " + ficheros.length);
          
          CoreSession documentManager = getCoreSession("default");
          if (!busy)
          {
            busy = true;
            for (int i = 0; i < ficheros.length; i++)
            {
              File file = new File(path + "/" + ficheros[i]);
              if (!file.isDirectory()) {
                if (documentIsOpen(file))
                {
                  log.warn("The document is open... it will be not uploaded yet");
                }
                else
                {
                  FileBlob blob = new FileBlob(file);
                  
                  File temporallocal = new File("/tmp/tmplocal" + System.currentTimeMillis());
                  
                  blob.transferTo(temporallocal);
                  
                  FileBlob blob2 = new FileBlob(temporallocal);
                  
                  String type = Framework.getProperty("AFM.DocumentType", "File");
                  
                  DocumentModel docModel = documentManager.createDocumentModel(pathWorkspace, IdUtils.generateId(file.getName() + System.currentTimeMillis()), type);
                  
                  docModel.setProperty("dublincore", "title", file.getName());
                  
                  docModel.setProperty("file", "filename", file.getName());
                  
                  docModel.setProperty("file", "content", blob2);
                  
                  docModel = documentManager.createDocument(docModel);
                  
                  Map<String, Serializable> options = new HashMap();
                  options.put("filePath", file.getAbsolutePath());
                  try
                  {
                    log.info("firing Event: documentCaptured");
                    AthentoNXUtils.notifyEvent("documentCaptured", docModel, options, null, null, true, false);
                  }
                  catch (Exception e)
                  {
                    log.error("Error firing Event: ", e);
                  }
                  String upVersion = Framework.getProperty("AFM.UpVersion", "false");
                  log.info("AFM.UpVersion." + upVersion);
                  if (upVersion.equals("true")) {
                    documentManager.checkIn(docModel.getRef(), VersioningOption.MINOR, null);
                  }
                  documentManager.save();
                  
                  TransactionHelper.commitOrRollbackTransaction();
                  TransactionHelper.startTransaction();
                  
                  file.delete();
                  
                  temporallocal.delete();
                  
                  log.info("Document saved: " + file.getName());
                }
              }
            }
            busy = false;
          }
        }
      }
      catch (NumberFormatException e)
      {
        busy = false;
        log.error("Error monitorizing folder... property \"AFM.NumberOfFolders\" is not a number", e);
      }
      catch (Exception e)
      {
        busy = false;
        log.error("Error monitorizing folder... ", e);
      }
    }
  }
  
  private boolean documentIsOpen(File document)
  {
    String[] command = { "sudo", "lsof", document.getAbsolutePath() };
    try
    {
      log.info("executing command '" + command[0] + " " + command[1] + " " + command[2] + "'");
      Process proc = Runtime.getRuntime().exec(command);
      
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      
      String line = bufferedReader.readLine();
      if (line != null)
      {
        log.info("Document is right now being used by:");
        line = bufferedReader.readLine();
        while (line != null)
        {
          log.info("'" + line.split(" ")[0].toString() + "'");
          line = bufferedReader.readLine();
        }
        return true;
      }
      return false;
    }
    catch (Exception e)
    {
      log.error("EXCEPTION:");
      e.printStackTrace();
    }
    return false;
  }
  
  private static CoreSession getCoreSession(String repo)
    throws ClientException
  {
    CoreSession systemSession;
    try
    {
      Framework.login();
      RepositoryManager manager = (RepositoryManager)Framework.getService(RepositoryManager.class);
      Repository repository = manager.getRepository(repo);
      if (repository == null)
      {
        log.info("repository " + repo + " not in available repos: " + manager.getRepositories());
        throw new ClientException("cannot get repository: " + repo);
      }
      systemSession = repository.open();
    }
    catch (ClientException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new ClientException("Failed to open core session to repository " + repo, e);
    }
    return systemSession;
  }
}

