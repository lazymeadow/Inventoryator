function toggleNewInventory() {
    $('#new-inventory').toggle();
    $('#inventory-form').toggle();
}

function toggleNewItem() {
    $('#new-item').toggle();
    $('#item-form').toggle();
}

function showDeleteButton() {
    if ($('input:checked').length > 0) {
        $('#delete_button').show();
    }
    else {
        $('#delete_button').hide();
    }
}

function deleteItems() {
    var deletes = $.map($('input:checked'), function (item) {
        return $.ajax({
            url: '/item/' + $(item).val(),
            method: 'DELETE'
        });
    });

    $.when.apply($, deletes).then(function () {
        window.location.reload();
    });
}
