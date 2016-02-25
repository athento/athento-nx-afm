package org.athento.ecm.athento.afm.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.yerbabuena.athento.ecm.utils.AthentoNXUtils;

/**
 * General listener for AFM.
 * 
 * @contributor victorsanchez
 * 
 */
public class GeneralListener implements EventListener {

	private static final Log log = LogFactory.getLog(GeneralListener.class);

	private static final String GENERAL_LISTENER = "generalServiceEvent";

	private static final String CAPTURED_EVENT = "documentCaptured";

	private static final String NUMBER_OF_FOLDERS = Framework
			.getProperty("AFM.NumberOfFolders");

	private static String OS = System.getProperty("os.name").toLowerCase();

	private double totalDocuments;
	private double totalLength;

	// private static final String FILE_EXTENSION = ".pdf";

	static boolean busy = false;

	public void handleEvent(Event event) throws ClientException {
		EventContext ctx = event.getContext();
		CoreSession documentManager = null;
		boolean sessionMustBeClosed = false;
		try {
			if (!(ctx instanceof DocumentEventContext)) {
				documentManager = getCoreSession("default");
				sessionMustBeClosed = true;
			} else {
				documentManager = ctx.getCoreSession();
			}
			if (event.getName().equals(GENERAL_LISTENER)) {
				afmRecursive(documentManager);
			}
		} finally {
			if (sessionMustBeClosed) {
				if (documentManager != null) {
					CoreInstance server = CoreInstance.getInstance();
					server.close(documentManager);
				}
			}
		}
	}

	private void afmRecursive(CoreSession coreSession) {

		totalDocuments = 0;
		totalLength = 0;

		try {

			int numberOfFolders = Integer.parseInt(NUMBER_OF_FOLDERS);
			if (log.isDebugEnabled()) {
				log.debug("AFM.NumberOfFolders: " + numberOfFolders);
			}
			for (int folderNumber = 1; folderNumber <= numberOfFolders; folderNumber++) {

				String path = Framework.getProperty("AFM.PathDocuments."
						+ folderNumber);
				if (log.isDebugEnabled()) {
					log.debug("AFM.PathDocuments." + folderNumber + ": " + path);
				}
				DocumentRef workspaceDestinyRef = null;
				String idWorkspace = Framework.getProperty("AFM.IdWorkspace."
						+ folderNumber);
				if (idWorkspace == null) {
					idWorkspace = Framework.getProperty("AFM.PathWorkspace."
							+ folderNumber);
					workspaceDestinyRef = new PathRef(idWorkspace);
				} else {
					workspaceDestinyRef = new IdRef(idWorkspace);
				}
				if (log.isDebugEnabled()) {
					log.debug("AFM.IdWorkspace." + folderNumber + ": "
							+ idWorkspace);
				}
				// Save start time of document upload
				long uploadProccessStart = System.currentTimeMillis();

				// Run in folder
				folderIterator(path, workspaceDestinyRef, coreSession);

				// Save end time of document upload
				long uploadProccessEnd = System.currentTimeMillis();

				double uploadProccessTime = uploadProccessEnd
						- uploadProccessStart;

				uploadProccessTime = uploadProccessTime / 1000.0;

				if (totalDocuments > 0) {

					double averageTimePerDocument = uploadProccessTime
							/ totalDocuments;
					if (log.isDebugEnabled()) {
						log.debug("\n\n===========================================================");
						log.debug("Upload proccess finished. Elapsed time: "
								+ uploadProccessTime + " secs to upload "
								+ totalDocuments + " documents (" + totalLength
								/ 1000000.0 + " MB).");
						log.debug("Average upload time per document: "
								+ averageTimePerDocument + " secs.");
						log.debug("===========================================================\n\n");
					}
				}
			}
		} catch (NumberFormatException e) {
			busy = false;
			log.error(
					"Error monitorizing folder... property \"AFM.NumberOfFolders\" is not a number",
					e);
		} catch (Exception e) {
			busy = false;
			log.error("Error monitorizing folder... ", e);
		}
	}

	private void folderIterator(
		String path, DocumentRef parentFolderRef, CoreSession documentManager)
			throws ClientException, IOException {
		File directorio = new File(path);
		String[] ficheros = directorio.list();
		if (log.isDebugEnabled()) {
			log.debug("Number of Documents in folder '" + path + "': "
					+ ficheros.length);
		}

		// Parent folder document
		DocumentModel parentFolder = documentManager
				.getDocument(parentFolderRef);

		// if(!busy){

		busy = true;

		for (int i = 0; i < ficheros.length; i++) {

			File file = null;

			if (isWindows()) {
				file = new File(path + "\\" + ficheros[i]);
			} else {
				file = new File(path + "/" + ficheros[i]);
			}

			if (file.isDirectory()) {

				// Save start time of document upload
				long uploadDocumentStart = System.currentTimeMillis();

				if (log.isInfoEnabled()) {
					log.info("Uploading folder '" + file.getAbsolutePath()
							+ "' to '" + parentFolder.getPathAsString() + "' ");
				}
				// File temporallocal = new
				// File("/tmp/tmplocal"+System.currentTimeMillis()/*+FILE_EXTENSION*/);

				String sufix = file.getName();

				DocumentModel docModel = documentManager.createDocumentModel(
						parentFolder.getPathAsString(),
						IdUtils.generateStringId(), "Folder");

				// Set title to document
				docModel.setProperty("dublincore", "title", sufix);

				// Set content to document
				docModel.setProperty("file", "filename", sufix);

				// Create document in repository
				docModel = documentManager.createDocument(docModel);

				// Fire captured event
				// throwDocumentCapturedEvent(file, docModel);

				// To increase the minor version of document
				incrementMinorVersion(documentManager, docModel);

				documentManager.save();

				TransactionHelper.commitOrRollbackTransaction();
				TransactionHelper.startTransaction();

				folderIterator(file.getAbsolutePath(), docModel.getRef(), documentManager);

				file.delete();

				// Save end time of document upload
				long uploadDocumentEnd = System.currentTimeMillis();

				double estimatedTime = uploadDocumentEnd - uploadDocumentStart;

				estimatedTime = estimatedTime / 1000.0;

				if (log.isInfoEnabled()) {
					log.info("Folder saved. Elapsed time: " + estimatedTime
							+ " secs.");
				}
			} else {
				if (documentIsOpen(file)) {
					log.warn("The document is open... it will not be uploaded yet");
				} else {

					// Save start time of document upload
					long uploadDocumentStart = System.currentTimeMillis();

					FileBlob blob = new FileBlob(file);

					totalLength += file.length();

					// File temporallocal = new
					// File("/tmp/tmplocal"+System.currentTimeMillis()/*+FILE_EXTENSION*/);

					String sufix = file.getName();
					File temporallocal = File.createTempFile("tmplocalAFM_",
							sufix);

					blob.transferTo(temporallocal);

					FileBlob blob2 = new FileBlob(temporallocal);

					if (log.isInfoEnabled()) {
						log.info("Uploading document '"
								+ file.getAbsolutePath() + "' to '"
								+ parentFolder.getPathAsString() + "' ");
					}
					String doctype = Framework.getProperty("AFM.DocumentType",
							"File");

					DocumentModel docModel = documentManager
							.createDocumentModel(
									parentFolder.getPathAsString(),
									IdUtils.generateStringId(), doctype);

					// Set title to document
					docModel.setProperty("dublincore", "title", sufix);

					// Set content to document
					docModel.setProperty("file", "filename", sufix);
					docModel.setProperty("file", "content", blob2);

					// Create document in repository
					docModel = documentManager.createDocument(docModel);

					// Fire captured event
					// throwDocumentCapturedEvent(file, docModel);

					// To increase the minor version of document
					incrementMinorVersion(documentManager, docModel);

					documentManager.save();

					TransactionHelper.commitOrRollbackTransaction();
					TransactionHelper.startTransaction();

					file.delete();

					temporallocal.delete();

					// Save end time of document upload
					long uploadDocumentEnd = System.currentTimeMillis();

					double estimatedTime = uploadDocumentEnd
							- uploadDocumentStart;

					estimatedTime = estimatedTime / 1000.0;

					if (log.isInfoEnabled()) {
						log.info("Document saved. Elapsed time: "
								+ estimatedTime + " secs.");
					}
				}
			}

			totalDocuments++;
			// }

			busy = false;
		}
	}

	private void afmOneLevel() {
		CoreSession documentManager = null;
		try {
			int numberOfFolders = Integer.parseInt(NUMBER_OF_FOLDERS);
			log.info("AFM.NumberOfFolders: " + numberOfFolders);

			for (int folderNumber = 1; folderNumber <= numberOfFolders; folderNumber++) {

				String path = Framework.getProperty("AFM.PathDocuments."
						+ folderNumber);
				log.info("AFM.PathDocuments." + folderNumber + ": " + path);

				String pathWorkspace = Framework
						.getProperty("AFM.PathWorkspace." + folderNumber);
				log.info("AFM.PathWorkspace." + folderNumber + ": "
						+ pathWorkspace);

				File directorio = new File(path);
				String[] ficheros = directorio.list();

				log.info("Number of Documents in folder: " + ficheros.length);

				if (!busy) {

					busy = true;

					// Save start time of document upload
					long uploadProccessStart = System.currentTimeMillis();

					for (int i = 0; i < ficheros.length; i++) {

						File file = null;

						if (isWindows()) {
							file = new File(path + "\\" + ficheros[i]);
						} else {
							file = new File(path + "/" + ficheros[i]);
						}

						if (file.isDirectory()) {
							continue;
						}

						if (documentIsOpen(file)) {
							log.warn("The document is open... it will be not uploaded yet");
						} else {

							// Save start time of document upload
							long uploadDocumentStart = System
									.currentTimeMillis();

							log.info("Uploading document '" + file.getName()
									+ "' to '" + pathWorkspace + "' ");

							FileBlob blob = new FileBlob(file);

							// File temporallocal = new
							// File("/tmp/tmplocal"+System.currentTimeMillis()/*+FILE_EXTENSION*/);

							String sufix = file.getName();
							File temporallocal = File.createTempFile(
									"tmplocalAFM_", sufix);

							blob.transferTo(temporallocal);

							FileBlob blob2 = new FileBlob(temporallocal);

							String type = Framework.getProperty(
									"AFM.DocumentType", "File");

							DocumentModel docModel = documentManager
									.createDocumentModel(pathWorkspace,
											IdUtils.generateStringId(), type);

							// Set title to document
							docModel.setProperty("dublincore", "title", sufix);

							// Set content to document
							docModel.setProperty("file", "filename", sufix);
							docModel.setProperty("file", "content", blob2);

							// Create document in repository
							docModel = documentManager.createDocument(docModel);

							// Fire captured event
							throwDocumentCapturedEvent(file, docModel);

							// To increase the minor version of document
							incrementMinorVersion(documentManager, docModel);

							documentManager.save();

							TransactionHelper.commitOrRollbackTransaction();
							TransactionHelper.startTransaction();

							file.delete();

							temporallocal.delete();

							// Save end time of document upload
							long uploadDocumentEnd = System.currentTimeMillis();

							double estimatedTime = uploadDocumentEnd
									- uploadDocumentStart;

							estimatedTime = estimatedTime / 1000.0;

							log.info("Document saved. Elapsed time: "
									+ estimatedTime + " secs.");
						}
					}

					busy = false;

					// Save end time of document upload
					long uploadProccessEnd = System.currentTimeMillis();

					double uploadProccessTime = uploadProccessEnd
							- uploadProccessStart;

					uploadProccessTime = uploadProccessTime / 1000.0;

					log.info("\n\n===========================================================");
					log.info("Upload proccess finished. Elapsed time: "
							+ uploadProccessTime + " secs.");
					log.info("===========================================================\n\n");
				}
			}
		} catch (NumberFormatException e) {
			busy = false;
			log.error(
					"Error monitorizing folder... property \"AFM.NumberOfFolders\" is not a number",
					e);
		} catch (Exception e) {
			busy = false;
			log.error("Error monitorizing folder... ", e);
		}
	}

	private void incrementMinorVersion(CoreSession documentManager,
			DocumentModel docModel) throws ClientException {
		String upVersion = Framework.getProperty("AFM.UpVersion", "false");
		log.info("AFM.UpVersion." + upVersion);
		if (upVersion.equals("true")) {
			documentManager.checkIn(docModel.getRef(), VersioningOption.MINOR,
					null);
		}
	}

	private void throwDocumentCapturedEvent(File file, DocumentModel docModel) {
		Map<String, Serializable> options = new HashMap<String, Serializable>();
		options.put("filePath", file.getAbsolutePath());

		try {
			log.info("firing Event: " + CAPTURED_EVENT);
			AthentoNXUtils.notifyEvent(CAPTURED_EVENT, docModel, options, null,
					null, true, false);
		} catch (Exception e) {
			log.error("Error firing Event: ", e);
		}
	}

	private boolean documentIsOpen(File document) {

		String[] command = { "lsof", document.getAbsolutePath() };

		try {
			Process proc = Runtime.getRuntime().exec(command);

			// CHECKING STANDARD OUTPUT
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));
			String line;

			line = bufferedReader.readLine();
			if (line != null) {
				log.info("Document is right now being used by:");
				line = bufferedReader.readLine();
				while (line != null) {
					log.info("'" + line.split(" ")[0].toString() + "'");
					line = bufferedReader.readLine();
				}
				return true;
			}

			// everything's OK
			return false;
		} catch (Exception e) {
			log.error("EXCEPTION:");
			e.printStackTrace();
			return false;
		}
	}

	public static boolean isWindows() {

		return (OS.indexOf("win") >= 0);

	}

	public static boolean isMac() {

		return (OS.indexOf("mac") >= 0);

	}

	public static boolean isUnix() {

		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS
				.indexOf("aix") > 0);

	}

	private static CoreSession getCoreSession(String repo)
			throws ClientException {

		CoreSession systemSession = null;
		try {
			Framework.login();
			RepositoryManager manager = Framework
					.getService(RepositoryManager.class);
			Repository repository = manager.getRepository(repo);
			if (repository == null) {
				log.info("repository " + repo + " not in available repos: "
						+ manager.getRepositories());
				throw new ClientException("cannot get repository: " + repo);
			}
			systemSession = repository.open();
		} catch (ClientException e) {
			throw e;
		} catch (Exception e) {
			throw new ClientException(
					"Failed to open core session to repository " + repo, e);
		}
		return systemSession;
	}

}
