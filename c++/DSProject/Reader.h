#ifndef READER
#define READER

#include <QThread>
#include "globals.h"
#include "Client.h"

class Reader : public QThread{

	Q_OBJECT

public:
    Reader(Client* xmlRpcClient);

	void run() Q_DECL_OVERRIDE;

private:
	void selectedOption(QString option);

	bool busy;

signals:
    void join(QString newNodeIP);
    void signOff();
    void startElection();
    void startConcatProcess();
    void stopOperations();
    void runOverRpc(QString functionName, QList<QVariant> in_params);
    void showAllLists();
    void echo(QString newNodeIP, QString echoStr);

	private slots:
	void clientFinished();
};

#endif //READER
