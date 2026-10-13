const OrderStatus = {
  Pending: "PENDING",
  Paid: "PAID",
  Shipped: "SHIPPED",
  Delivered: "DELIVERED",
  Cancelled: "CANCELLED",
} as const;

type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

interface Order {
  id: number;
  status: OrderStatus;
}

function nextStatus(status: OrderStatus): OrderStatus {
  switch (status) {
    case OrderStatus.Pending:
      return OrderStatus.Paid;
    case OrderStatus.Paid:
      return OrderStatus.Shipped;
    case OrderStatus.Shipped:
      return OrderStatus.Delivered;
    case OrderStatus.Delivered:
    case OrderStatus.Cancelled:
      return status;
  }
}

function isFinal(status: OrderStatus): boolean {
  return status === OrderStatus.Delivered || status === OrderStatus.Cancelled;
}

let order: Order = { id: 1, status: OrderStatus.Pending };
console.log("start:", order.status, "final?", isFinal(order.status));

for (let step = 0; step < 3; step++) {
  order = { ...order, status: nextStatus(order.status) };
  console.log("next:", order.status, "final?", isFinal(order.status));
}

const allStatuses: OrderStatus[] = Object.values(OrderStatus);
console.log("all statuses:", allStatuses.join(", "));
